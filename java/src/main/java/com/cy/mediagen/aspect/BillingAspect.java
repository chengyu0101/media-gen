package com.cy.mediagen.aspect;

import com.cy.mediagen.annotation.DeductPoints;
import com.cy.mediagen.context.TenantContextHolder;
import com.cy.mediagen.exception.InsufficientBalanceException;
import com.cy.mediagen.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 算力计费 AOP 切面
 * <p>
 * 拦截标有 @DeductPoints 注解的 Controller 方法：
 * 1. 执行前：预扣算力（乐观锁）
 * 2. 执行业务逻辑
 * 3. 异常补偿：业务失败时自动退还已扣算力
 * 保证计费与业务的最终一致性。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Aspect
@Order(200)
@Component
@RequiredArgsConstructor
public class BillingAspect {

    private final TenantService tenantService;

    /**
     * 环绕通知：拦截所有标有 @DeductPoints 的方法
     *
     * @param joinPoint    切入点
     * @param deductPoints 注解信息
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(deductPoints)")
    public Object aroundDeductPoints(ProceedingJoinPoint joinPoint, DeductPoints deductPoints) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        int points = deductPoints.value();

        if (tenantId == null) {
            log.error("计费切面执行失败：租户上下文为空，方法: {}", joinPoint.getSignature().toShortString());
            throw new IllegalStateException("无法识别当前租户，请检查请求头中的租户信息");
        }

        log.info("计费切面 - 预扣算力开始，tenantId: {}，扣减点数: {}，方法: {}",
                tenantId, points, joinPoint.getSignature().toShortString());

        // ============ 步骤一：预扣算力（Before） ============
        try {
            tenantService.deductBalance(tenantId, points);
        } catch (InsufficientBalanceException e) {
            log.warn("计费切面 - 算力余额不足，拦截请求，tenantId: {}，需扣减: {}", tenantId, points);
            throw e;
        }

        // ============ 步骤二：执行业务逻辑（Proceed） ============
        try {
            Object result = joinPoint.proceed();
            log.info("计费切面 - 业务执行成功，算力扣减确认，tenantId: {}，扣减: {}", tenantId, points);
            return result;
        } catch (Throwable ex) {
            // ============ 步骤三：异常补偿（AfterThrowing） ============
            log.error("计费切面 - 业务执行失败，执行算力退还，tenantId: {}，退还点数: {}，异常: {}",
                    tenantId, points, ex.getMessage());
            try {
                tenantService.refundBalance(tenantId, points);
                log.info("计费切面 - 算力退还成功，tenantId: {}，退还: {}", tenantId, points);
            } catch (Exception refundEx) {
                // 退费失败记录告警日志，人工介入
                log.error("【严重告警】算力退还失败！需人工处理，tenantId: {}，退还点数: {}，退费异常: {}",
                        tenantId, points, refundEx.getMessage(), refundEx);
            }
            throw ex;
        }
    }
}
