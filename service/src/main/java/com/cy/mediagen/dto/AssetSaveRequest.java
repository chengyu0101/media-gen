package com.cy.mediagen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 资产元数据保存请求 DTO
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "资产元数据保存请求")
public class AssetSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "资产类型不能为空")
    @Schema(description = "资产类型：AVATAR_IMAGE-人像底图，BG_IMAGE-背景图，AUDIO-音频", example = "AVATAR_IMAGE")
    private String assetType;

    @NotBlank(message = "OSS 对象 Key 不能为空")
    @Schema(description = "OSS 对象唯一标识 Key", example = "tenant_1001/assets/abc123.jpg")
    private String objectKey;
}
