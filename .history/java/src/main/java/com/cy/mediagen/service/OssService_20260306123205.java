package com.cy.mediagen.service;

import com.cy.mediagen.dto.PreSignedUrlDTO;

/**
 * 阿里云 OSS 服务接口（客户端直传架构）
 *
 * @author cy
 * @date 2026-03-06
 */
public interface OssService {

    /**
     * 生成客户端直传预签名上传 URL
     * <p>
     * 客户端拿到此 URL 后，直接向阿里云 OSS 发起 HTTP PUT 请求上传文件，
     * 不经过 Spring Boot 后端中转，确保 IO 性能最大化。
     * </p>
     *
     * @param fileName 原始文件名（用于提取扩展名）
     * @param tenantId 租户ID（用于构建 OSS 目录隔离）
     * @return 包含预签名 URL 和 objectKey 的 DTO
     */
    PreSignedUrlDTO generatePreSignedUploadUrl(String fileName, Long tenantId);

    /**
     * 生成私有对象的临时访问链接
     * <p>
     * 由于 Bucket 权限为私有读写，此方法生成带签名的临时 GET 链接，
     * 供系统后台或大模型 API 拉取底图时使用。
     * </p>
     *
     * @param objectKey OSS 对象唯一标识 Key
     * @return 带签名的临时访问 URL
     */
    String generatePreSignedDownloadUrl(String objectKey);
}
