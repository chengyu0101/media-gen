package com.cy.mediagen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 渲染任务提交请求 DTO
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频渲染任务提交请求")
public class RenderTaskRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "人像底图资产ID不能为空")
    @Schema(description = "人像底图资产 ID（asset_library 主键）", example = "1")
    private Long sourceAssetId;

    @Schema(description = "门店背景图资产 ID（可为空）", example = "2")
    private Long bgAssetId;

    @Schema(description = "合成好的语音 OSS 链接", example = "https://bucket.oss.aliyuncs.com/tenant_1001/assets/voice.mp3")
    private String audioUrl;
}
