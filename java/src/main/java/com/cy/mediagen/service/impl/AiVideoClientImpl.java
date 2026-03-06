package com.cy.mediagen.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cy.mediagen.config.AiProperties;
import com.cy.mediagen.dto.RenderResultDTO;
import com.cy.mediagen.service.AiVideoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 视频渲染客户端实现类（对接外部 I2V 大模型 API）
 * <p>
 * 当前实现基于通义千问视频生成 API（DashScope 异步任务模式）。
 * 若后续切换为 Seedance 等其他平台，仅需替换此实现类，业务层无感。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiVideoClientImpl implements AiVideoClient {

    private final WebClient webClient;
    private final AiProperties aiProperties;

    /** 异步任务提交 API 路径 */
    private static final String SUBMIT_TASK_PATH = "/api/v1/services/aigc/image2video/generation";

    /** 异步任务状态查询 API 路径 */
    private static final String QUERY_TASK_PATH = "/api/v1/tasks/";

    /** DashScope 启用异步模式的请求头 */
    private static final String ASYNC_HEADER = "X-DashScope-Async";

    /** 渲染状态映射 — DashScope 返回值 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";

    /** 默认视频生成参数 */
    private static final int DEFAULT_DURATION = 5;
    private static final double DEFAULT_CFG_SCALE = 7.0;
    private static final String DEFAULT_RESOLUTION = "1080x1920";

    @Override
    public String submitRenderTask(String sourceImageUrl, String audioUrl) {
        if (StrUtil.isBlank(sourceImageUrl)) {
            throw new IllegalArgumentException("人像底图 URL 不能为空");
        }

        log.info("提交外部视频渲染任务，人像底图: {}，语音: {}", sourceImageUrl, audioUrl);

        // 构建请求体
        Map<String, Object> requestBody = buildSubmitRequestBody(sourceImageUrl, audioUrl);

        try {
            String apiUrl = aiProperties.getDashscope().getBaseUrl().replace("/compatible-mode/v1", "")
                    + SUBMIT_TASK_PATH;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getDashscope().getApiKey())
                    .header(ASYNC_HEADER, "enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // 提取外部任务 ID
            String externalTaskId = extractTaskId(response);
            log.info("外部视频渲染任务提交成功，externalTaskId: {}", externalTaskId);
            return externalTaskId;

        } catch (Exception e) {
            log.error("提交外部视频渲染任务失败，错误: {}", e.getMessage(), e);
            throw new RuntimeException("提交外部视频渲染任务失败", e);
        }
    }

    @Override
    public RenderResultDTO pollRenderStatus(String externalTaskId) {
        if (StrUtil.isBlank(externalTaskId)) {
            throw new IllegalArgumentException("外部任务ID不能为空");
        }

        log.debug("轮询外部渲染任务状态，externalTaskId: {}", externalTaskId);

        try {
            String apiUrl = aiProperties.getDashscope().getBaseUrl().replace("/compatible-mode/v1", "")
                    + QUERY_TASK_PATH + externalTaskId;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getDashscope().getApiKey())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseRenderResult(response);

        } catch (Exception e) {
            log.error("轮询外部渲染任务状态失败，externalTaskId: {}，错误: {}",
                    externalTaskId, e.getMessage(), e);
            return RenderResultDTO.builder()
                    .status("FAILED")
                    .errorMessage("轮询渲染状态异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 构建提交渲染任务的请求体
     */
    private Map<String, Object> buildSubmitRequestBody(String sourceImageUrl, String audioUrl) {
        Map<String, Object> input = new HashMap<>(4);
        input.put("image_url", sourceImageUrl);
        if (StrUtil.isNotBlank(audioUrl)) {
            input.put("audio_url", audioUrl);
        }

        Map<String, Object> parameters = new HashMap<>(8);
        parameters.put("duration", DEFAULT_DURATION);
        parameters.put("cfg_scale", DEFAULT_CFG_SCALE);
        parameters.put("resolution", DEFAULT_RESOLUTION);

        Map<String, Object> requestBody = new HashMap<>(4);
        requestBody.put("model", "image2video-turbo");
        requestBody.put("input", input);
        requestBody.put("parameters", parameters);

        return requestBody;
    }

    /**
     * 从提交响应中提取外部任务 ID
     */
    @SuppressWarnings("unchecked")
    private String extractTaskId(Map<String, Object> response) {
        if (response == null) {
            throw new RuntimeException("外部 API 返回空响应");
        }

        // DashScope 异步任务返回格式：{ "output": { "task_id": "xxx", "task_status": "PENDING" }
        // }
        var output = (Map<String, Object>) response.get("output");
        if (output == null) {
            throw new RuntimeException("外部 API 响应中缺少 output 字段，响应: " + response);
        }

        String taskId = (String) output.get("task_id");
        if (StrUtil.isBlank(taskId)) {
            throw new RuntimeException("外部 API 响应中缺少 task_id，响应: " + response);
        }

        return taskId;
    }

    /**
     * 解析轮询查询的渲染结果
     */
    @SuppressWarnings("unchecked")
    private RenderResultDTO parseRenderResult(Map<String, Object> response) {
        if (response == null) {
            return RenderResultDTO.builder()
                    .status("FAILED")
                    .errorMessage("轮询 API 返回空响应")
                    .build();
        }

        var output = (Map<String, Object>) response.get("output");
        if (output == null) {
            return RenderResultDTO.builder()
                    .status("FAILED")
                    .errorMessage("轮询 API 响应中缺少 output 字段")
                    .build();
        }

        String taskStatus = (String) output.get("task_status");
        var builder = RenderResultDTO.builder();

        return switch (taskStatus) {
            case STATUS_SUCCEEDED -> {
                // 成功时提取视频 URL
                String videoUrl = extractVideoUrl(output);
                log.info("外部渲染任务完成，视频URL: {}", videoUrl);
                yield builder.status("SUCCESS").rawVideoUrl(videoUrl).build();
            }
            case STATUS_FAILED -> {
                String errMsg = (String) output.getOrDefault("message", "外部渲染失败，未返回具体错误");
                log.error("外部渲染任务失败: {}", errMsg);
                yield builder.status("FAILED").errorMessage(errMsg).build();
            }
            case STATUS_PENDING, STATUS_RUNNING -> {
                log.debug("外部渲染任务进行中，状态: {}", taskStatus);
                yield builder.status("PROCESSING").build();
            }
            default -> {
                log.warn("外部渲染任务返回未知状态: {}", taskStatus);
                yield builder.status("PROCESSING").build();
            }
        };
    }

    /**
     * 从成功响应中提取视频 URL
     */
    @SuppressWarnings("unchecked")
    private String extractVideoUrl(Map<String, Object> output) {
        // DashScope 格式：output.results[0].url
        var results = (List<Map<String, Object>>) output.get("results");
        if (results != null && !results.isEmpty()) {
            return (String) results.get(0).get("url");
        }

        // 兼容格式：output.video_url
        String videoUrl = (String) output.get("video_url");
        if (StrUtil.isNotBlank(videoUrl)) {
            return videoUrl;
        }

        throw new RuntimeException("渲染成功但无法提取视频URL，output: " + output);
    }
}
