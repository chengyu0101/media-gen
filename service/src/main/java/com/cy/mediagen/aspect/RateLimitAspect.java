package com.cy.mediagen.aspect;

import cn.hutool.core.util.StrUtil;
import com.cy.mediagen.annotation.RateLimit;
import com.cy.mediagen.context.TenantContextHolder;
import com.cy.mediagen.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流 AOP 切面（基于 Redis 固定窗口计数器）
 * <p>
 * 以租户维度进行限流控制，使用 Redis INCR + EXPIRE 实现固定窗口限流。
 * 拦截标有 @RateLimit 注解的方法，超过阈值返回 HTTP 429。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Aspect
@Order(100)
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 限流 Redis Key 前缀 */
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * 环绕通知：拦截所有标有 @RateLimit 的方法
     *
     * @param joinPoint 切入点
     * @param rateLimit 注解信息
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(rateLimit)")
    public Object aroundRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("限流切面：租户上下文为空，跳过限流检查");
            return joinPoint.proceed();
        }

        int maxRequests = rateLimit.maxRequests();
        int timeWindow = rateLimit.timeWindow();
        String methodName = joinPoint.getSignature().toShortString();

        // 构建 Redis Key：rate_limit:{method}:{tenantId}
        String rateLimitKey = StrUtil.format("{}{}:{}",
                RATE_LIMIT_KEY_PREFIX,
                joinPoint.getSignature().getDeclaringType().getSimpleName()
                        + "." + joinPoint.getSignature().getName(),
                tenantId);

        // Redis 原子操作：INCR + 首次设置过期时间
        Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (currentCount == null) {
            log.warn("限流切面：Redis INCR 返回 null，跳过限流检查");
            return joinPoint.proceed();
        }

        // 首次访问时设置过期时间（固定窗口起点）
        if (currentCount == 1L) {
            redisTemplate.expire(rateLimitKey, timeWindow, TimeUnit.SECONDS);
        }

        // 检查是否超过阈值
        if (currentCount > maxRequests) {
            log.warn("接口限流触发，tenantId: {}，方法: {}，当前次数: {}，阈值: {}/{}秒",
                    tenantId, methodName, currentCount, maxRequests, timeWindow);
            throw new RateLimitExceededException(
                    StrUtil.format("请求过于频繁，每 {} 秒最多允许 {} 次请求，请稍后再试",
                            timeWindow, maxRequests));
        }

        log.debug("限流检查通过，tenantId: {}，方法: {}，当前次数: {}/{}",
                tenantId, methodName, currentCount, maxRequests);

        return joinPoint.proceed();
    }
}
