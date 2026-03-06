package com.cy.mediagen.service;

/**
 * FFmpeg 视频合成服务接口
 * <p>
 * 利用 FFmpeg 执行底层音视频处理，包括绿幕抠像、背景融合、重新编码等操作。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
public interface FfmpegService {

    /**
     * 融合人像视频与门店背景图
     * <p>
     * 将大模型生成的原始人像视频（绿幕/透明通道）叠加到经过高斯模糊处理的门店背景图上，
     * 输出兼容各大短视频平台的 MP4 文件。
     * </p>
     *
     * @param rawVideoPath    大模型生成的原始人像视频本地临时路径
     * @param bgImagePath     门店背景图本地临时路径
     * @param outputVideoPath 合成后的最终输出路径
     * @return 是否合成成功
     */
    boolean mergeVideoAndBackground(String rawVideoPath, String bgImagePath, String outputVideoPath);
}
