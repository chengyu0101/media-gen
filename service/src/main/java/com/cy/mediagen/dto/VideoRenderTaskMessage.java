package com.cy.mediagen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频渲染任务消息体（Redis 队列传输对象）
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRenderTaskMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务表主键ID */
    private Long taskId;

    /** 租户ID */
    private Long tenantId;

    /** 人像底图 OSS 访问地址 */
    private String sourceAssetUrl;

    /** 门店背景图 OSS 访问地址（可为空） */
    private String bgAssetUrl;

    /** 合成好的语音 OSS 链接 */
    private String audioUrl;
}
