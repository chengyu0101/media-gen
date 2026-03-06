package com.cy.mediagen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性类
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /** OSS 服务端点 */
    private String endpoint;

    /** 访问密钥 ID */
    private String accessKeyId;

    /** 访问密钥 Secret */
    private String accessKeySecret;

    /** Bucket 名称 */
    private String bucketName;
}
