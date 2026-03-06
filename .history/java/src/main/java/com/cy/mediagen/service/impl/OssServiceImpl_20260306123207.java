package com.cy.mediagen.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.cy.mediagen.config.OssProperties;
import com.cy.mediagen.dto.PreSignedUrlDTO;
import com.cy.mediagen.service.OssService;
import com.aliyun.oss.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 服务实现类（客户端直传架构）
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OSS ossClient;
    private final OssProperties ossProperties;

    /** 上传预签名 URL 有效期：15 分钟（毫秒） */
    private static final long UPLOAD_URL_EXPIRATION_MS = 15 * 60 * 1000L;

    /** 下载预签名 URL 有效期：30 分钟（毫秒） */
    private static final long DOWNLOAD_URL_EXPIRATION_MS = 30 * 60 * 1000L;

    /** 租户目录前缀模版 */
    private static final String TENANT_DIR_TEMPLATE = "tenant_{}/";

    /** 默认子目录 */
    private static final String DEFAULT_SUB_DIR = "assets/";

    @Override
    public PreSignedUrlDTO generatePreSignedUploadUrl(String fileName, Long tenantId) {
        if (StrUtil.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 构建 OSS 对象 Key：tenant_{id}/assets/{uuid}.{ext}
        var extension = FileUtil.extName(fileName);
        var uniqueFileName = IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(extension) ? "." + extension : "");
        var objectKey = StrUtil.format(TENANT_DIR_TEMPLATE, tenantId) + DEFAULT_SUB_DIR + uniqueFileName;

        // 设置预签名 URL 过期时间
        var expiration = new Date(System.currentTimeMillis() + UPLOAD_URL_EXPIRATION_MS);

        // 生成 HTTP PUT 预签名链接
        var request = new GeneratePresignedUrlRequest(ossProperties.getBucketName(), objectKey, HttpMethod.PUT);
        request.setExpiration(expiration);

        URL signedUrl = ossClient.generatePresignedUrl(request);

        log.info("生成客户端直传预签名URL成功，租户ID: {}，objectKey: {}，有效期: 15分钟", tenantId, objectKey);

        return PreSignedUrlDTO.builder()
                .uploadUrl(signedUrl.toString())
                .objectKey(objectKey)
                .build();
    }

    @Override
    public String generatePreSignedDownloadUrl(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new IllegalArgumentException("objectKey 不能为空");
        }

        // 设置预签名 URL 过期时间
        var expiration = new Date(System.currentTimeMillis() + DOWNLOAD_URL_EXPIRATION_MS);

        // 生成 HTTP GET 预签名链接（私有读取）
        URL signedUrl = ossClient.generatePresignedUrl(ossProperties.getBucketName(), objectKey, expiration);

        log.info("生成私有对象临时访问链接成功，objectKey: {}，有效期: 30分钟", objectKey);

        return signedUrl.toString();
    }
}
