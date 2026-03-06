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
 * 合规文案生成请求 DTO
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "合规文案生成请求")
public class ScriptGenerateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "营销意图不能为空")
    @Schema(description = "营销意图", example = "推广水光针")
    private String intent;

    @NotBlank(message = "行业类型不能为空")
    @Schema(description = "行业类型", example = "医美")
    private String industry;
}
