package com.cy.mediagen.service;

import com.cy.mediagen.dto.RenderResultDTO;

/**
 * AI 视频渲染客户端接口（防腐层 — 对接外部 I2V 大模型）
 * <p>
 * 隔离第三方高保真图像转视频 API（如 Seedance、通义千问视频生成）的变动。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
public interface AiVideoClient {

    /**
     * 提交外部视频渲染任务
     *
     * @param sourceImageUrl 人像底图 OSS 访问链接
     * @param audioUrl       已合成的驱动语音 OSS 链接
     * @return 第三方平台返回的外部任务 ID
     */
    String submitRenderTask(String sourceImageUrl, String audioUrl);

    /**
     * 轮询查询外部渲染任务状态
     * <p>
     * 注意：此方法为单次查询，不会阻塞循环。轮询策略由调用方（Consumer）控制。
     * </p>
     *
     * @param externalTaskId 第三方平台的任务 ID
     * @return 渲染结果 DTO（包含状态和原始视频 URL）
     */
    RenderResultDTO pollRenderStatus(String externalTaskId);
}
