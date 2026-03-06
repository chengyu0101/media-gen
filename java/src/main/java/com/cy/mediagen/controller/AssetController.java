package com.cy.mediagen.controller;

import com.cy.mediagen.context.TenantContextHolder;
import com.cy.mediagen.dto.AssetSaveRequest;
import com.cy.mediagen.dto.PreSignedUrlDTO;
import com.cy.mediagen.dto.Result;
import com.cy.mediagen.entity.AssetLibrary;
import com.cy.mediagen.service.AssetLibraryService;
import com.cy.mediagen.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数字资产管理接口
 * <p>
 * 提供 OSS 直传签名获取和资产元数据入库功能。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assets")
@Tag(name = "资产管理", description = "数字资产上传与管理接口")
public class AssetController {

    private final OssService ossService;
    private final AssetLibraryService assetLibraryService;

    /**
     * 获取 OSS 客户端直传预签名 URL
     */
    @GetMapping("/upload-url")
    @Operation(summary = "获取上传签名 URL", description = "生成 OSS 客户端直传预签名 PUT URL，前端拿到后直接上传文件到阿里云")
    public Result<PreSignedUrlDTO> getUploadUrl(
            @Parameter(description = "资产类型：AVATAR_IMAGE/BG_IMAGE/AUDIO", required = true) @RequestParam String assetType,
            @Parameter(description = "原始文件名", required = true) @RequestParam String fileName) {

        Long tenantId = requireTenantId();
        log.info("获取上传签名 URL，tenantId: {}，资产类型: {}，文件名: {}", tenantId, assetType, fileName);

        PreSignedUrlDTO result = ossService.generatePreSignedUploadUrl(fileName, tenantId);
        return Result.success(result);
    }

    /**
     * 保存资产元数据（前端直传 OSS 成功后调用）
     */
    @PostMapping
    @Operation(summary = "保存资产元数据", description = "前端完成 OSS 直传后，调用此接口将物料元数据保存到数据库")
    public Result<Long> saveAsset(@Valid @RequestBody AssetSaveRequest request) {
        Long tenantId = requireTenantId();
        log.info("保存资产元数据，tenantId: {}，类型: {}，objectKey: {}",
                tenantId, request.getAssetType(), request.getObjectKey());

        // 生成资产访问链接
        String assetUrl = ossService.generatePreSignedDownloadUrl(request.getObjectKey());

        // 构建实体并入库
        var asset = AssetLibrary.builder()
                .tenantId(tenantId)
                .assetType(request.getAssetType())
                .objectKey(request.getObjectKey())
                .assetUrl(assetUrl)
                .build();
        assetLibraryService.save(asset);

        log.info("资产元数据保存成功，assetId: {}，tenantId: {}", asset.getId(), tenantId);
        return Result.success("资产保存成功", asset.getId());
    }

    /**
     * 从 TenantContextHolder 获取当前租户 ID（严禁前端传递）
     */
    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("无法识别当前租户，请确认请求头中包含租户信息");
        }
        return tenantId;
    }
}
