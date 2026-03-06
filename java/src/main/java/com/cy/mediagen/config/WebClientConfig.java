package com.cy.mediagen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置类（用于调用第三方 AI API）
 *
 * @author cy
 * @date 2026-03-06
 */
@Configuration
public class WebClientConfig {

    /**
     * 创建通用 WebClient Bean
     *
     * @return WebClient.Builder 实例
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
