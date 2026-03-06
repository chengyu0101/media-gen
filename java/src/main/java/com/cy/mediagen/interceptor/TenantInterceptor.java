package com.cy.mediagen.interceptor;

import cn.hutool.core.util.StrUtil;
import com.cy.mediagen.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 多租户请求拦截器
 * <p>
 * 在请求到达 Controller 之前，从 HTTP Header 中解析 tenantId，
 * 放入 TenantContextHolder 供业务链路使用。
 * 请求结束后强制清理 ThreadLocal，防止内存泄漏。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    /** 租户 ID 请求头名称 */
    private static final String TENANT_HEADER = "X-Tenant-Id";

    /** Authorization 请求头名称 */
    private static final String AUTH_HEADER = "Authorization";

    /** Bearer Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        // 优先从 X-Tenant-Id 头获取（MVP 阶段简化方案）
        String tenantIdStr = request.getHeader(TENANT_HEADER);

        // 如果没有 X-Tenant-Id，尝试从 JWT Token 解析（预留）
        if (StrUtil.isBlank(tenantIdStr)) {
            tenantIdStr = parseTenantIdFromToken(request);
        }

        if (StrUtil.isNotBlank(tenantIdStr)) {
            try {
                Long tenantId = Long.parseLong(tenantIdStr);
                TenantContextHolder.setTenantId(tenantId);
                log.debug("租户上下文已设置，tenantId: {}，请求路径: {}",
                        tenantId, request.getRequestURI());
            } catch (NumberFormatException e) {
                log.warn("无效的租户ID格式: {}，请求路径: {}", tenantIdStr, request.getRequestURI());
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        // 极度重要：必须清理 ThreadLocal，防止线程池复用导致内存泄漏和数据串租
        TenantContextHolder.clear();
    }

    /**
     * 从 JWT Token 中解析租户 ID（预留实现）
     * <p>
     * MVP 阶段暂时返回 null，后续接入 JWT 解析后在此处实现。
     * </p>
     *
     * @param request HTTP 请求
     * @return 租户 ID 字符串
     */
    private String parseTenantIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            // TODO: JWT 解析，提取 tenantId claim
            // String token = authHeader.substring(BEARER_PREFIX.length());
            // return JwtUtils.parseTenantId(token);
            log.debug("JWT Token 解析租户ID预留，当前 MVP 阶段跳过");
        }
        return null;
    }
}
