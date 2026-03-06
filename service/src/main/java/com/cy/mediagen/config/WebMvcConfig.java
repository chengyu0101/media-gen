package com.cy.mediagen.config;

import com.cy.mediagen.interceptor.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类（注册拦截器）
 *
 * @author cy
 * @date 2026-03-06
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                // 拦截所有 API 请求
                .addPathPatterns("/api/**")
                // 排除健康检查等公共路径
                .excludePathPatterns("/api/health", "/api/public/**");
    }
}
