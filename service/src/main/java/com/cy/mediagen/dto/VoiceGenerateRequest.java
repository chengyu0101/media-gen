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
 * TTS 语音合成请求 DTO
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TTS 语音合成请求")
public class VoiceGenerateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "口播文案不能为空")
    @Schema(description = "口播文案文本")
    private String text;

    @Schema(description = "音色模型 ID（为空使用默认音色）", example = "sambert-zhichu-v1")
    private String voiceId;
}
