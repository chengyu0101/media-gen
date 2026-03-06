package com.cy.mediagen.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.cy.mediagen.config.AiProperties;
import com.cy.mediagen.service.AiContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 内容生成服务实现类（防腐层）
 * <p>
 * 封装通义千问 DashScope API（文案生成）和 TTS API（语音合成），
 * 隔离第三方接口变动对业务层的影响。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiContentServiceImpl implements AiContentService {

    private final WebClient webClient;
    private final AiProperties aiProperties;

    /** 合规文案生成 System Prompt — 医美行业专用 */
    private static final String COMPLIANT_SCRIPT_SYSTEM_PROMPT = """
            你是一个严谨的医美行业营销文案专家。请根据用户的意图生成一段 30 秒内的短视频口播文案。
            你必须严格遵守中国《广告法》及医疗美容广告监管规定，绝对禁止使用以下类型的词汇：
            1. 极限词：如"第一"、"最好"、"全网最低"、"独一无二"等；
            2. 虚假承诺：如"包治"、"彻底去皱"、"永久美白"、"零风险"等；
            3. 绝对化用语：如"100%有效"、"立竿见影"、"无副作用"等。
            文案风格要求：高端、专业、有亲和力。
            输出格式：直接输出文案内容，不需要任何前缀或解释说明。
            """;

    /** 临时文件存放目录 */
    private static final String TEMP_AUDIO_DIR = System.getProperty("java.io.tmpdir") + File.separator
            + "media-gen-audio";

    /** API 请求超时时间提示 */
    private static final int API_TIMEOUT_SECONDS = 30;

    @Override
    public String generateCompliantScript(String intent, String industry) {
        if (StrUtil.isBlank(intent)) {
            throw new IllegalArgumentException("营销意图（intent）不能为空");
        }
        if (StrUtil.isBlank(industry)) {
            throw new IllegalArgumentException("行业类型（industry）不能为空");
        }

        log.info("开始生成合规营销文案，意图: {}，行业: {}", intent, industry);

        // 构建用户消息
        String userMessage = StrUtil.format("请为「{}」行业生成一段关于「{}」的短视频口播文案。", industry, intent);

        // 构建符合 OpenAI 兼容格式的请求体
        Map<String, Object> requestBody = buildChatRequestBody(userMessage);

        try {
            // 调用通义千问 DashScope 兼容 API
            String apiUrl = aiProperties.getDashscope().getBaseUrl() + "/chat/completions";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getDashscope().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // 解析响应
            String content = extractContentFromResponse(response);
            log.info("合规营销文案生成成功，意图: {}，文案长度: {} 字", intent, content.length());
            return content;

        } catch (Exception e) {
            log.error("调用大模型 API 生成文案失败，意图: {}，错误: {}", intent, e.getMessage(), e);
            throw new RuntimeException("合规文案生成失败，请稍后重试", e);
        }
    }

    @Override
    public String generateSpeech(String text, String voiceId) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("口播文案文本不能为空");
        }

        // 音色模型为空时使用默认配置
        String actualVoiceId = StrUtil.isNotBlank(voiceId) ? voiceId : aiProperties.getTts().getDefaultVoiceId();
        log.info("开始 TTS 语音合成，音色模型: {}，文本长度: {} 字", actualVoiceId, text.length());

        // 构建 TTS 请求体
        Map<String, Object> requestBody = buildTtsRequestBody(text, actualVoiceId);

        try {
            // 确保临时目录存在
            FileUtil.mkdir(TEMP_AUDIO_DIR);
            String tempFileName = IdUtil.fastSimpleUUID() + ".mp3";
            Path tempFilePath = Path.of(TEMP_AUDIO_DIR, tempFileName);

            // 调用 TTS API，以流式方式下载音频
            var dataBufferFlux = webClient.post()
                    .uri(aiProperties.getTts().getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getDashscope().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            // 写入本地临时文件
            DataBufferUtils.write(dataBufferFlux, tempFilePath, StandardOpenOption.CREATE_NEW)
                    .block();

            log.info("TTS 语音合成成功，音频文件: {}，音色模型: {}", tempFilePath, actualVoiceId);
            return tempFilePath.toString();

        } catch (Exception e) {
            log.error("TTS 语音合成失败，音色模型: {}，错误: {}", actualVoiceId, e.getMessage(), e);
            throw new RuntimeException("TTS 语音合成失败，请稍后重试", e);
        }
    }

    /**
     * 构建大模型 Chat 请求体（OpenAI 兼容格式）
     *
     * @param userMessage 用户消息
     * @return 请求体 Map
     */
    private Map<String, Object> buildChatRequestBody(String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>(2);

        Map<String, String> systemMsg = new HashMap<>(2);
        systemMsg.put("role", "system");
        systemMsg.put("content", COMPLIANT_SCRIPT_SYSTEM_PROMPT);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>(2);
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>(4);
        requestBody.put("model", aiProperties.getDashscope().getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);

        return requestBody;
    }

    /**
     * 从大模型响应中提取文案内容
     *
     * @param response API 响应
     * @return 文案内容
     */
    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(Map<String, Object> response) {
        if (response == null) {
            throw new RuntimeException("大模型 API 返回空响应");
        }

        var choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("大模型 API 响应中未包含有效的 choices 数据");
        }

        var firstChoice = choices.get(0);
        var message = (Map<String, Object>) firstChoice.get("message");
        if (message == null) {
            throw new RuntimeException("大模型 API 响应中未包含有效的 message 数据");
        }

        String content = (String) message.get("content");
        if (StrUtil.isBlank(content)) {
            throw new RuntimeException("大模型 API 返回空文案内容");
        }

        return content.trim();
    }

    /**
     * 构建 TTS 请求体（DashScope TTS 格式）
     *
     * @param text    文案文本
     * @param voiceId 音色模型 ID
     * @return 请求体 Map
     */
    private Map<String, Object> buildTtsRequestBody(String text, String voiceId) {
        Map<String, Object> input = new HashMap<>(2);
        input.put("text", text);

        Map<String, Object> parameters = new HashMap<>(4);
        parameters.put("voice", voiceId);
        parameters.put("format", "mp3");
        parameters.put("sample_rate", 16000);

        Map<String, Object> requestBody = new HashMap<>(4);
        requestBody.put("model", "sambert-v1");
        requestBody.put("input", input);
        requestBody.put("parameters", parameters);

        return requestBody;
    }
}
