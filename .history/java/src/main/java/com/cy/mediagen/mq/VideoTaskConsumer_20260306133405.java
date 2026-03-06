package com.cy.mediagen.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cy.mediagen.constant.RedisQueueConst;
import com.cy.mediagen.dto.RenderResultDTO;
import com.cy.mediagen.dto.VideoRenderTaskMessage;
import com.cy.mediagen.entity.VideoTask;
import com.cy.mediagen.enums.TaskStatusEnum;
import com.cy.mediagen.mapper.VideoTaskMapper;
import com.cy.mediagen.service.AiVideoClient;
import com.cy.mediagen.service.FfmpegService;
import com.cy.mediagen.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 视频渲染任务消费者（完整消费链路）
 * <p>
 * 消费链路：
 * 1. 从 Redis 队列阻塞拉取任务
 * 2. 下载物料（底图、语音）到本地 /tmp
 * 3. 提交外部 AI 渲染任务
 * 4. 带超时机制的轮询等待渲染完成
 * 5. 下载原始视频 → FFmpeg 融合门店背景
 * 6. 成品上传 OSS → 更新数据库
 * 7. finally 清理所有临时文件
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTaskConsumer implements CommandLineRunner {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VideoTaskMapper videoTaskMapper;
    private final AiVideoClient aiVideoClient;
    private final FfmpegService ffmpegService;
    private final OssService ossService;

    /** 错误日志最大长度，防止超出数据库 TEXT 字段合理范围 */
    private static final int MAX_ERROR_LOG_LENGTH = 2000;

    /** 外部渲染任务最大等待时间（分钟） */
    private static final int RENDER_POLL_MAX_MINUTES = 10;

    /** 轮询间隔（秒） */
    private static final int RENDER_POLL_INTERVAL_SECONDS = 10;

    /** 临时文件根目录 */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "media-gen-render";

    /** 成品视频 OSS 目录模板 */
    private static final String OUTPUT_VIDEO_OSS_DIR = "tenant_{}/videos/";

    @Override
    public void run(String... args) {
        // 启动守护线程执行消费逻辑
        Thread consumerThread = new Thread(this::consumeLoop, "video-task-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("视频渲染任务消费者线程已启动，队列: {}", RedisQueueConst.VIDEO_RENDER_TASK_QUEUE);
    }

    /**
     * 消费主循环：阻塞式拉取 Redis 队列任务
     */
    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 阻塞式拉取（BRPOP 语义），超时后重试
                Object rawMessage = redisTemplate.opsForList().rightPop(
                        RedisQueueConst.VIDEO_RENDER_TASK_QUEUE,
                        RedisQueueConst.QUEUE_POP_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS);

                if (rawMessage == null) {
                    continue;
                }

                // 反序列化消息
                var message = JSONUtil.toBean(rawMessage.toString(), VideoRenderTaskMessage.class);
                log.info("消费者拉取到视频渲染任务，taskId: {}，tenantId: {}", message.getTaskId(), message.getTenantId());

                // 更新任务状态为渲染中
                updateTaskStatus(message.getTaskId(), TaskStatusEnum.PROCESSING, null, null);

                // 执行完整渲染链路
                processRenderTask(message);

            } catch (Exception e) {
                log.error("视频渲染任务消费循环发生异常: {}", e.getMessage(), e);
                sleepQuietly(3000L);
            }
        }
        log.warn("视频渲染任务消费者线程已退出");
    }

    /**
     * 完整的渲染处理链路
     * <p>
     * 关键设计：所有临时文件路径收集到 tempFiles 列表，
     * 无论成功还是失败，finally 块中统一清理，防止磁盘写满。
     * </p>
     *
     * @param message 任务消息
     */
    private void processRenderTask(VideoRenderTaskMessage message) {
        Long taskId = message.getTaskId();
        Long tenantId = message.getTenantId();

        // 收集所有临时文件，finally 统一清理
        List<String> tempFiles = new ArrayList<>(8);

        try {
            // 创建任务专属临时目录
            String taskTempDir = TEMP_DIR + File.separator + "task_" + taskId;
            FileUtil.mkdir(taskTempDir);

            // ======== 步骤一：下载物料到本地 /tmp ========
            log.info("[步骤1/6] 下载物料到本地，taskId: {}", taskId);

            String localSourceImage = downloadMaterial(
                    message.getSourceAssetUrl(), taskTempDir, "source_image", tempFiles);

            String localBgImage = null;
            if (StrUtil.isNotBlank(message.getBgAssetUrl())) {
                localBgImage = downloadMaterial(
                        message.getBgAssetUrl(), taskTempDir, "bg_image", tempFiles);
            }

            String localAudio = null;
            if (StrUtil.isNotBlank(message.getAudioUrl())) {
                localAudio = downloadMaterial(
                        message.getAudioUrl(), taskTempDir, "audio", tempFiles);
            }

            // ======== 步骤二：提交外部 AI 渲染任务 ========
            log.info("[步骤2/6] 提交外部 AI 渲染任务，taskId: {}", taskId);

            String externalTaskId = aiVideoClient.submitRenderTask(
                    message.getSourceAssetUrl(), message.getAudioUrl());
            log.info("外部渲染任务已提交，taskId: {}，externalTaskId: {}", taskId, externalTaskId);

            // ======== 步骤三：带超时机制的轮询等待 ========
            log.info("[步骤3/6] 轮询等待外部渲染完成，taskId: {}，最大等待: {} 分钟",
                    taskId, RENDER_POLL_MAX_MINUTES);

            RenderResultDTO renderResult = pollUntilFinished(externalTaskId, taskId);

            if (!renderResult.isSuccess()) {
                throw new RuntimeException("外部 AI 渲染失败: " + renderResult.getErrorMessage());
            }

            // ======== 步骤四：下载原始视频 + FFmpeg 合成 ========
            log.info("[步骤4/6] 下载原始视频并执行 FFmpeg 合成，taskId: {}", taskId);

            String localRawVideo = Path.of(taskTempDir, "raw_video.mp4").toString();
            downloadFromUrl(renderResult.getRawVideoUrl(), localRawVideo);
            tempFiles.add(localRawVideo);

            String localOutputVideo = Path.of(taskTempDir, "output_final.mp4").toString();
            tempFiles.add(localOutputVideo);

            boolean mergeSuccess;
            if (StrUtil.isNotBlank(localBgImage)) {
                // 有背景图时执行 FFmpeg 融图
                mergeSuccess = ffmpegService.mergeVideoAndBackground(
                        localRawVideo, localBgImage, localOutputVideo);
            } else {
                // 无背景图时直接使用原始视频作为成品
                FileUtil.copy(localRawVideo, localOutputVideo, true);
                mergeSuccess = true;
                log.info("无门店背景图，跳过 FFmpeg 合成，直接使用原始视频");
            }

            if (!mergeSuccess) {
                throw new RuntimeException("FFmpeg 视频融图合成失败");
            }

            // ======== 步骤五：成品上传 OSS ========
            log.info("[步骤5/6] 上传成品视频到 OSS，taskId: {}", taskId);

            String outputObjectKey = StrUtil.format(OUTPUT_VIDEO_OSS_DIR, tenantId)
                    + IdUtil.fastSimpleUUID() + ".mp4";
            String resultVideoUrl = ossService.uploadFile(localOutputVideo, outputObjectKey);

            // ======== 步骤六：更新数据库为成功 ========
            log.info("[步骤6/6] 更新数据库任务状态为成功，taskId: {}", taskId);
            updateTaskStatus(taskId, TaskStatusEnum.SUCCESS, null, resultVideoUrl);

            log.info("===== 视频渲染任务全部完成，taskId: {}，视频链接: {} =====", taskId, resultVideoUrl);

        } catch (Exception e) {
            log.error("视频渲染任务处理失败，taskId: {}，错误: {}", taskId, e.getMessage(), e);
            String errorLog = StrUtil.sub(ExceptionUtil.stacktraceToString(e), 0, MAX_ERROR_LOG_LENGTH);
            updateTaskStatus(taskId, TaskStatusEnum.FAILED, errorLog, null);
        } finally {
            // ======== 极度重要：清理所有临时文件 ========
            cleanupTempFiles(tempFiles, taskId);
        }
    }

    /**
     * 下载物料文件到本地
     *
     * @param url       物料 URL（OSS 签名链接或外部链接）
     * @param tempDir   临时目录
     * @param prefix    文件名前缀
     * @param tempFiles 临时文件列表（用于 finally 清理）
     * @return 本地文件路径
     */
    private String downloadMaterial(String url, String tempDir, String prefix, List<String> tempFiles) {
        String extension = StrUtil.subAfter(url, ".", true);
        // 截取扩展名中查询参数部分
        if (StrUtil.contains(extension, "?")) {
            extension = StrUtil.subBefore(extension, "?", false);
        }
        if (StrUtil.isBlank(extension) || extension.length() > 10) {
            extension = "dat";
        }

        String localPath = Path.of(tempDir, prefix + "_" + IdUtil.fastSimpleUUID() + "." + extension).toString();
        cn.hutool.http.HttpUtil.downloadFile(url, localPath);
        tempFiles.add(localPath);
        log.info("物料下载完成，类型: {}，本地路径: {}", prefix, localPath);
        return localPath;
    }

    /**
     * 从指定 URL 下载文件到本地
     *
     * @param url       下载地址
     * @param localPath 本地保存路径
     */
    private void downloadFromUrl(String url, String localPath) {
        cn.hutool.http.HttpUtil.downloadFile(url, localPath);
        log.info("文件下载完成，本地路径: {}", localPath);
    }

    /**
     * 带超时机制的轮询策略
     * <p>
     * 每隔 {@link #RENDER_POLL_INTERVAL_SECONDS} 秒查询一次外部任务状态，
     * 最大等待 {@link #RENDER_POLL_MAX_MINUTES} 分钟，超时则视为失败。
     * </p>
     *
     * @param externalTaskId 外部任务 ID
     * @param taskId         内部任务 ID（用于日志）
     * @return 渲染结果
     */
    private RenderResultDTO pollUntilFinished(String externalTaskId, Long taskId) {
        long startTime = System.currentTimeMillis();
        long maxWaitMs = RENDER_POLL_MAX_MINUTES * 60 * 1000L;
        int pollCount = 0;

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            sleepQuietly(RENDER_POLL_INTERVAL_SECONDS * 1000L);
            pollCount++;

            RenderResultDTO result = aiVideoClient.pollRenderStatus(externalTaskId);
            log.info("第 {} 次轮询外部渲染状态，taskId: {}，externalTaskId: {}，状态: {}",
                    pollCount, taskId, externalTaskId, result.getStatus());

            if (result.isFinished()) {
                return result;
            }
        }

        // 超时
        log.error("外部渲染任务超时（{} 分钟），taskId: {}，externalTaskId: {}",
                RENDER_POLL_MAX_MINUTES, taskId, externalTaskId);
        return RenderResultDTO.builder()
                .status("FAILED")
                .errorMessage(StrUtil.format("外部渲染任务超时，已等待 {} 分钟", RENDER_POLL_MAX_MINUTES))
                .build();
    }

    /**
     * 更新任务状态
     *
     * @param taskId         任务ID
     * @param status         目标状态
     * @param errorLog       错误日志（仅 FAILED 时有值）
     * @param resultVideoUrl 视频结果链接（仅 SUCCESS 时有值）
     */
    private void updateTaskStatus(Long taskId, TaskStatusEnum status, String errorLog, String resultVideoUrl) {
        var updateEntity = new VideoTask();
        updateEntity.setId(taskId);
        updateEntity.setTaskStatus(status.getCode());
        if (StrUtil.isNotBlank(errorLog)) {
            updateEntity.setErrorLog(errorLog);
        }
        if (StrUtil.isNotBlank(resultVideoUrl)) {
            updateEntity.setResultVideoUrl(resultVideoUrl);
        }
        videoTaskMapper.updateById(updateEntity);
        log.info("任务状态已更新为 {}，taskId: {}", status.getDescription(), taskId);
    }

    /**
     * 清理所有临时文件（防止磁盘写满）
     * <p>
     * 必须在 finally 块中调用，即使渲染失败也要执行清理。
     * </p>
     *
     * @param tempFiles 临时文件路径列表
     * @param taskId    任务 ID（用于日志）
     */
    private void cleanupTempFiles(List<String> tempFiles, Long taskId) {
        log.info("开始清理临时文件，taskId: {}，文件数: {}", taskId, tempFiles.size());
        int cleanedCount = 0;

        for (String filePath : tempFiles) {
            try {
                if (FileUtil.exist(filePath)) {
                    FileUtil.del(filePath);
                    cleanedCount++;
                }
            } catch (Exception e) {
                log.warn("清理临时文件失败，路径: {}，错误: {}", filePath, e.getMessage());
            }
        }

        // 尝试清理任务专属临时目录
        String taskTempDir = TEMP_DIR + File.separator + "task_" + taskId;
        try {
            if (FileUtil.exist(taskTempDir)) {
                FileUtil.del(taskTempDir);
            }
        } catch (Exception e) {
            log.warn("清理任务临时目录失败，目录: {}，错误: {}", taskTempDir, e.getMessage());
        }

        log.info("临时文件清理完成，taskId: {}，已清理: {} 个文件", taskId, cleanedCount);
    }

    /**
     * 安静休眠，不抛出中断异常
     *
     * @param millis 休眠毫秒数
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("消费者线程休眠被中断");
        }
    }
}
