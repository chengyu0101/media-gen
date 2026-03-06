package com.cy.mediagen.context;

/**
 * 多租户上下文持有器（基于 ThreadLocal）
 * <p>
 * SaaS 系统核心工具类，用于在请求链路中传递当前租户 ID，
 * 确保数据隔离，防止 A 商家查到 B 商家数据。
 * </p>
 * <p>
 * 注意：必须在请求结束后调用 {@link #clear()} 清理 ThreadLocal，
 * 防止线程池复用导致的内存泄漏和数据串租。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
public final class TenantContextHolder {

    private TenantContextHolder() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前请求的租户 ID
     *
     * @param tenantId 租户 ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取当前请求的租户 ID
     *
     * @return 租户 ID（可能为 null）
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 清理当前线程的租户上下文（防止内存泄漏）
     * <p>
     * 必须在 HandlerInterceptor#afterCompletion 中调用
     * </p>
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
