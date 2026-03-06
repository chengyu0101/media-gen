package com.cy.mediagen.service;

import com.cy.mediagen.dto.PreSignedUrlDTO;

/**
 * 阿里云 OSS 服务接口（客户端直传 + 服务端操作）
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

    /**
     * 服务端上传本地文件到 OSS
     * <p>
     * 用于将服务端本地生成的成品视频上传至 OSS 存储。
     * </p>
     *
     * @param localFilePath 本地文件绝对路径
     * @param objectKey     OSS 对象 Key
     * @return 上传后的访问 URL
     */
    String uploadFile(String localFilePath, String objectKey);

    /**
     * 从 OSS 下载对象到本地临时文件
     * <p>
     * 用于消费者下载底图、语音等物料到本地 /tmp 目录进行处理。
     * </p>
     *
     * @param objectKey     OSS 对象 Key
     * @param localFilePath 本地保存路径
     */
    void downloadToLocal(String objectKey, String localFilePath);
}
