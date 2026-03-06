package com.cy.mediagen.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 客户端配置类
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OssConfig {

    private final OssProperties ossProperties;

    /**
     * 创建阿里云 OSS 客户端 Bean
     *
     * @return OSS 客户端实例
     */
    @Bean
    public OSS ossClient() {
        log.info("初始化阿里云 OSS 客户端，Endpoint: {}", ossProperties.getEndpoint());
        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());
    }
}
