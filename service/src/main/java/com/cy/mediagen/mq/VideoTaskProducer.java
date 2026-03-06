package com.cy.mediagen.mq;

import cn.hutool.json.JSONUtil;
import com.cy.mediagen.constant.RedisQueueConst;
import com.cy.mediagen.dto.VideoRenderTaskMessage;
import com.cy.mediagen.entity.VideoTask;
import com.cy.mediagen.enums.TaskStatusEnum;
import com.cy.mediagen.mapper.VideoTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 视频渲染任务生产者（将任务压入 Redis 队列）
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTaskProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VideoTaskMapper videoTaskMapper;

    /**
     * 提交视频渲染任务到 Redis 队列
     * <p>
     * 1. 将消息序列化为 JSON 后压入 Redis List 左端
     * 2. 更新数据库中任务状态为 PENDING（排队中）
     * </p>
     *
     * @param message 视频渲染任务消息
     */
    public void submitTask(VideoRenderTaskMessage message) {
        if (message == null || message.getTaskId() == null) {
            throw new IllegalArgumentException("任务消息或任务ID不能为空");
        }

        // 序列化为 JSON 压入队列
        String jsonMessage = JSONUtil.toJsonStr(message);
        redisTemplate.opsForList().leftPush(RedisQueueConst.VIDEO_RENDER_TASK_QUEUE, jsonMessage);
        log.info("视频渲染任务已入队，taskId: {}，tenantId: {}", message.getTaskId(), message.getTenantId());

        // 更新数据库任务状态为排队中
        var updateEntity = new VideoTask();
        updateEntity.setId(message.getTaskId());
        updateEntity.setTaskStatus(TaskStatusEnum.PENDING.getCode());
        videoTaskMapper.updateById(updateEntity);
        log.info("任务状态已更新为 PENDING，taskId: {}", message.getTaskId());
    }
}
