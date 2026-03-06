package com.cy.mediagen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 外部大模型视频渲染结果 DTO
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 渲染状态：PROCESSING-处理中，SUCCESS-成功，FAILED-失败 */
    private String status;

    /** 原始视频下载地址（渲染成功时有值） */
    private String rawVideoUrl;

    /** 失败原因（渲染失败时有值） */
    private String errorMessage;

    /** 是否渲染完成（SUCCESS 或 FAILED） */
    public boolean isFinished() {
        return "SUCCESS".equals(status) || "FAILED".equals(status);
    }

    /** 是否渲染成功 */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
