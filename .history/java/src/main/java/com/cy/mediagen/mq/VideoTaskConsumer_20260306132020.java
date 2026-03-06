package com.cy.mediagen.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cy.mediagen.constant.RedisQueueConst;
import com.cy.mediagen.dto.VideoRenderTaskMessage;
import com.cy.mediagen.entity.VideoTask;
import com.cy.mediagen.enums.TaskStatusEnum;
import com.cy.mediagen.mapper.VideoTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 视频渲染任务消费者（长轮询阻塞式拉取 Redis 队列任务）
 * <p>
 * 使用 CommandLineRunner 启动独立线程，通过 rightPop 阻塞拉取任务。
 * 拉取到任务后更新状态为 PROCESSING，调用 AI 渲染逻辑；
 * 若发生异常，更新状态为 FAILED 并记录错误日志。
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

    /** 错误日志最大长度，防止超出数据库 TEXT 字段合理范围 */
    private static final int MAX_ERROR_LOG_LENGTH = 2000;

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
                    // 超时未拉取到任务，继续轮询
                    continue;
                }

                // 反序列化消息
                var message = JSONUtil.toBean(rawMessage.toString(), VideoRenderTaskMessage.class);
                log.info("消费者拉取到视频渲染任务，taskId: {}，tenantId: {}", message.getTaskId(), message.getTenantId());

                // 更新任务状态为渲染中
                updateTaskStatus(message.getTaskId(), TaskStatusEnum.PROCESSING, null);

                // 调用实际渲染逻辑（预留接口）
                processRenderTask(message);

            } catch (Exception e) {
                log.error("视频渲染任务消费循环发生异常: {}", e.getMessage(), e);
                // 短暂休眠避免异常风暴
                sleepQuietly(3000L);
            }
        }
        log.warn("视频渲染任务消费者线程已退出");
    }

    /**
     * 执行实际的视频渲染处理逻辑
     * <p>
     * TODO: 接入实际的 AI 视频渲染 API 调用
     * </p>
     *
     * @param message 任务消息
     */
    private void processRenderTask(VideoRenderTaskMessage message) {
        try {
            // ========================================
            // TODO: 此处接入实际的 AI 视频渲染逻辑
            // 1. 调用数字人视频合成 API
            // 2. 传入人像底图、背景图、音频等参数
            // 3. 获取渲染结果视频 URL
            // ========================================
            log.info("开始处理视频渲染任务，taskId: {}，sourceAssetUrl: {}",
                    message.getTaskId(), message.getSourceAssetUrl());

            // 模拟预留：渲染成功后更新任务状态
            // String resultVideoUrl = aiVideoApi.render(...);
            // updateTaskSuccess(message.getTaskId(), resultVideoUrl);

            log.info("视频渲染任务处理完成（预留逻辑），taskId: {}", message.getTaskId());

        } catch (Exception e) {
            log.error("视频渲染任务处理失败，taskId: {}，错误: {}", message.getTaskId(), e.getMessage(), e);
            // 异常时更新状态为 FAILED，并记录异常栈
            String errorLog = StrUtil.sub(ExceptionUtil.stacktraceToString(e), 0, MAX_ERROR_LOG_LENGTH);
            updateTaskStatus(message.getTaskId(), TaskStatusEnum.FAILED, errorLog);
        }
    }

    /**
     * 更新任务状态
     *
     * @param taskId   任务ID
     * @param status   目标状态
     * @param errorLog 错误日志（仅 FAILED 时有值）
     */
    private void updateTaskStatus(Long taskId, TaskStatusEnum status, String errorLog) {
        var updateEntity = new VideoTask();
        updateEntity.setId(taskId);
        updateEntity.setTaskStatus(status.getCode());
        if (StrUtil.isNotBlank(errorLog)) {
            updateEntity.setErrorLog(errorLog);
        }
        videoTaskMapper.updateById(updateEntity);
        log.info("任务状态已更新为 {}，taskId: {}", status.getDescription(), taskId);
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
