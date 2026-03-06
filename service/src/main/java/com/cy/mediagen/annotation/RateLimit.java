package com.cy.mediagen.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * <p>
 * 标记在 Controller 方法上，基于 Redis 实现固定窗口限流。
 * 以租户维度进行限流控制，防止恶意刷接口。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内最大请求次数，默认 5 次
     */
    int maxRequests() default 5;

    /**
     * 时间窗口大小（秒），默认 60 秒
     */
    int timeWindow() default 60;
}
