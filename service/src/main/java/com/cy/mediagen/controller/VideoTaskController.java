package com.cy.mediagen.controller;

import com.cy.mediagen.annotation.DeductPoints;
import com.cy.mediagen.annotation.RateLimit;
import com.cy.mediagen.context.TenantContextHolder;
import com.cy.mediagen.dto.RenderTaskRequest;
import com.cy.mediagen.dto.Result;
import com.cy.mediagen.dto.VideoRenderTaskMessage;
import com.cy.mediagen.dto.VideoTaskStatusDTO;
import com.cy.mediagen.entity.AssetLibrary;
import com.cy.mediagen.entity.VideoTask;
import com.cy.mediagen.enums.TaskStatusEnum;
import com.cy.mediagen.mapper.VideoTaskMapper;
import com.cy.mediagen.mq.VideoTaskProducer;
import com.cy.mediagen.service.AssetLibraryService;
import com.cy.mediagen.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 视频渲染任务调度接口（系统业务主入口）
 * <p>
 * 挂载 @DeductPoints 计费注解和 @RateLimit 限流注解，
 * 所有数据访问严格基于 TenantContextHolder 做租户隔离。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
@Tag(name = "视频渲染任务", description = "核心渲染调度接口（含计费与限流）")
public class VideoTaskController {

    private final VideoTaskMapper videoTaskMapper;
    private final VideoTaskProducer videoTaskProducer;
    private final AssetLibraryService assetLibraryService;
    private final OssService ossService;

    /**
     * 提交视频渲染任务
     * <p>
     * 预扣 10 算力 + 限流（60秒内最多5次）
     * </p>
     */
    @PostMapping("/render")
    @DeductPoints(10)
    @RateLimit(maxRequests = 5, timeWindow = 60)
    @Operation(summary = "提交渲染任务", description = "提交视频渲染任务到队列，预扣 10 算力点，限流 5次/分钟")
    public Result<Long> submitRenderTask(@Valid @RequestBody RenderTaskRequest request) {
        Long tenantId = requireTenantId();
        log.info("提交渲染任务，tenantId: {}，sourceAssetId: {}，bgAssetId: {}",
                tenantId, request.getSourceAssetId(), request.getBgAssetId());

        // ======== 校验资产归属 ========
        AssetLibrary sourceAsset = validateAssetOwnership(request.getSourceAssetId(), tenantId, "人像底图");

        String bgAssetUrl = null;
        if (request.getBgAssetId() != null) {
            AssetLibrary bgAsset = validateAssetOwnership(request.getBgAssetId(), tenantId, "门店背景图");
            bgAssetUrl = ossService.generatePreSignedDownloadUrl(bgAsset.getObjectKey());
        }

        // ======== 创建任务记录 ========
        var videoTask = VideoTask.builder()
                .tenantId(tenantId)
                .sourceAssetId(request.getSourceAssetId())
                .bgAssetId(request.getBgAssetId())
                .taskStatus(TaskStatusEnum.PENDING.getCode())
                .build();
        videoTaskMapper.insert(videoTask);

        // ======== 组装消息并压入队列 ========
        String sourceAssetUrl = ossService.generatePreSignedDownloadUrl(sourceAsset.getObjectKey());

        var message = VideoRenderTaskMessage.builder()
                .taskId(videoTask.getId())
                .tenantId(tenantId)
                .sourceAssetUrl(sourceAssetUrl)
                .bgAssetUrl(bgAssetUrl)
                .audioUrl(request.getAudioUrl())
                .build();
        videoTaskProducer.submitTask(message);

        log.info("渲染任务提交成功，taskId: {}，tenantId: {}", videoTask.getId(), tenantId);
        return Result.success("渲染任务提交成功", videoTask.getId());
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询任务状态", description = "查询指定任务的当前状态，严格校验租户归属")
    public Result<VideoTaskStatusDTO> getTaskStatus(
            @Parameter(description = "任务 ID", required = true) @PathVariable Long taskId) {

        Long tenantId = requireTenantId();
        log.info("查询任务状态，tenantId: {}，taskId: {}", tenantId, taskId);

        // 查询任务
        VideoTask task = videoTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在，taskId: " + taskId);
        }

        // ======== 严格校验租户归属，防止越权 ========
        if (!tenantId.equals(task.getTenantId())) {
            log.warn("【安全告警】租户越权访问任务，当前 tenantId: {}，任务归属 tenantId: {}，taskId: {}",
                    tenantId, task.getTenantId(), taskId);
            throw new IllegalArgumentException("任务不存在，taskId: " + taskId);
        }

        var statusDTO = VideoTaskStatusDTO.builder()
                .taskId(task.getId())
                .taskStatus(task.getTaskStatus())
                .resultVideoUrl(task.getResultVideoUrl())
                .errorLog(task.getErrorLog())
                .build();

        return Result.success(statusDTO);
    }

    /**
     * 校验资产归属当前租户
     *
     * @param assetId  资产 ID
     * @param tenantId 当前租户 ID
     * @param label    资产描述（日志用）
     * @return 校验通过的资产实体
     */
    private AssetLibrary validateAssetOwnership(Long assetId, Long tenantId, String label) {
        AssetLibrary asset = assetLibraryService.getById(assetId);
        if (asset == null) {
            throw new IllegalArgumentException(label + "资产不存在，assetId: " + assetId);
        }
        if (!tenantId.equals(asset.getTenantId())) {
            log.warn("【安全告警】租户越权访问资产，当前 tenantId: {}，资产归属 tenantId: {}，assetId: {}",
                    tenantId, asset.getTenantId(), assetId);
            throw new IllegalArgumentException(label + "资产不存在，assetId: " + assetId);
        }
        return asset;
    }

    /**
     * 从 TenantContextHolder 获取当前租户 ID
     */
    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("无法识别当前租户，请确认请求头中包含租户信息");
        }
        return tenantId;
    }
}
