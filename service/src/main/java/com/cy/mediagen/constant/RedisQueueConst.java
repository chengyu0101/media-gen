package com.cy.mediagen.constant;

/**
 * Redis 队列常量定义
 *
 * @author cy
 * @date 2026-03-06
 */
public final class RedisQueueConst {

    private RedisQueueConst() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /** 视频渲染任务队列 Key */
    public static final String VIDEO_RENDER_TASK_QUEUE = "queue:video:render:task";

    /** 队列消费者阻塞超时时间（秒） */
    public static final long QUEUE_POP_TIMEOUT_SECONDS = 5L;
}
