package com.cy.mediagen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频任务状态查询响应 DTO
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频任务状态响应")
public class VideoTaskStatusDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务 ID")
    private Long taskId;

    @Schema(description = "任务状态：PENDING-排队中，PROCESSING-渲染中，SUCCESS-成功，FAILED-失败")
    private String taskStatus;

    @Schema(description = "最终成片视频链接（SUCCESS 时有值）")
    private String resultVideoUrl;

    @Schema(description = "失败原因（FAILED 时有值）")
    private String errorLog;
}
