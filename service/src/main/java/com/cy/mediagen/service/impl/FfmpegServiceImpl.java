package com.cy.mediagen.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cy.mediagen.service.FfmpegService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 视频合成服务实现类（工业级进程管理）
 * <p>
 * 使用 ProcessBuilder 调用系统 FFmpeg 命令行工具，
 * 实现绿幕抠像/透明通道叠加、背景高斯模糊、重新编码等操作。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Service
public class FfmpegServiceImpl implements FfmpegService {

    /** FFmpeg 执行超时时间（分钟） */
    private static final int FFMPEG_TIMEOUT_MINUTES = 10;

    /** 目标视频分辨率宽 */
    private static final int TARGET_WIDTH = 1080;

    /** 目标视频分辨率高 */
    private static final int TARGET_HEIGHT = 1920;

    /** 高斯模糊强度 sigma 值（模拟大光圈景深效果） */
    private static final int GAUSSIAN_BLUR_SIGMA = 10;

    /** 绿幕抠像颜色相似度阈值 */
    private static final double CHROMAKEY_SIMILARITY = 0.15;

    /** 绿幕抠像混合阈值 */
    private static final double CHROMAKEY_BLEND = 0.1;

    @Override
    public boolean mergeVideoAndBackground(String rawVideoPath, String bgImagePath, String outputVideoPath) {
        if (StrUtil.isBlank(rawVideoPath) || StrUtil.isBlank(bgImagePath) || StrUtil.isBlank(outputVideoPath)) {
            throw new IllegalArgumentException("FFmpeg 合成参数不能为空：rawVideoPath、bgImagePath、outputVideoPath");
        }

        log.info("开始 FFmpeg 视频融图合成，原始视频: {}，背景图: {}，输出: {}",
                rawVideoPath, bgImagePath, outputVideoPath);

        // 构建 FFmpeg 命令
        List<String> command = buildFfmpegCommand(rawVideoPath, bgImagePath, outputVideoPath);

        log.info("FFmpeg 完整命令: {}", String.join(" ", command));

        Process process = null;
        try {
            var processBuilder = new ProcessBuilder(command);
            // 合并 stderr 到 stdout 方便统一采集日志
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // 读取 FFmpeg 输出日志（防止进程卡死在缓冲区写满）
            String processOutput = readProcessOutput(process);

            // 等待进程完成，设置超时
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (!finished) {
                log.error("FFmpeg 进程执行超时（{} 分钟），强制终止", FFMPEG_TIMEOUT_MINUTES);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("FFmpeg 视频融图合成成功，输出文件: {}", outputVideoPath);
                return true;
            } else {
                log.error("FFmpeg 进程退出码异常: {}，输出日志:\n{}", exitCode, processOutput);
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("FFmpeg 进程被中断", e);
            return false;
        } catch (Exception e) {
            log.error("FFmpeg 视频融图合成异常: {}", e.getMessage(), e);
            return false;
        } finally {
            // 确保进程被销毁
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                log.warn("FFmpeg 进程被强制销毁");
            }
        }
    }

    /**
     * 构建 FFmpeg 命令（绿幕抠像 + 背景模糊叠加 + 重编码）
     * <p>
     * 滤镜链设计说明：
     * 
     * <pre>
     * 输入0 (背景图):
     *   → scale={w}:{h}           缩放/裁剪至目标分辨率
     *   → setsar=1                重置像素宽高比
     *   → gblur=sigma={sigma}     高斯模糊，模拟大光圈景深
     *   → [bg]
     *
     * 输入1 (人像视频):
     *   → scale={w}:{h}           缩放至目标分辨率
     *   → chromakey=green:        绿幕抠像（若为绿幕源）
     *     similarity={sim}:
     *     blend={blend}
     *   → [fg]
     *
     * [bg][fg] → overlay            前景叠加到背景上
     *   → format=yuv420p           确保兼容性
     *   → [out]
     * </pre>
     * </p>
     *
     * @param rawVideoPath    原始人像视频路径
     * @param bgImagePath     背景图路径
     * @param outputVideoPath 输出路径
     * @return FFmpeg 命令参数列表
     */
    private List<String> buildFfmpegCommand(String rawVideoPath, String bgImagePath, String outputVideoPath) {
        var command = new ArrayList<String>(32);

        command.add("ffmpeg");
        // 覆盖已有输出文件
        command.add("-y");
        // 输入0：背景图（循环以匹配视频时长）
        command.add("-loop");
        command.add("1");
        command.add("-i");
        command.add(bgImagePath);
        // 输入1：人像视频
        command.add("-i");
        command.add(rawVideoPath);

        // 复合滤镜链
        String filterComplex = buildFilterComplex();
        command.add("-filter_complex");
        command.add(filterComplex);

        // 映射滤镜输出
        command.add("-map");
        command.add("[out]");
        // 同时映射人像视频的音轨
        command.add("-map");
        command.add("1:a?");

        // 视频编码器：libx264（兼容各大短视频平台）
        command.add("-c:v");
        command.add("libx264");
        // 编码预设：medium 平衡速度与质量
        command.add("-preset");
        command.add("medium");
        // CRF 质量参数：18-23 为高质量范围
        command.add("-crf");
        command.add("20");
        // 像素格式
        command.add("-pix_fmt");
        command.add("yuv420p");

        // 音频编码器：AAC
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");

        // 以最短输入流为基准结束
        command.add("-shortest");

        // 限制 H.264 Profile 和 Level 确保兼容性
        command.add("-profile:v");
        command.add("high");
        command.add("-level");
        command.add("4.1");
        // MOOV Atom 前置，方便网络播放
        command.add("-movflags");
        command.add("+faststart");

        // 输出文件
        command.add(outputVideoPath);

        return command;
    }

    /**
     * 构建复合滤镜表达式
     * <p>
     * 方案一（绿幕源）：chromakey 抠除绿幕后 overlay 到模糊背景
     * 方案二（透明通道）：直接 overlay 到模糊背景（FFmpeg 自动处理 Alpha 通道）
     * 当前默认使用 chromakey 方案，因为大多数 I2V 模型输出为绿幕视频
     * </p>
     *
     * @return 滤镜表达式字符串
     */
    private String buildFilterComplex() {
        // 背景处理：缩放 + 高斯模糊
        String bgFilter = StrUtil.format(
                "[0:v]scale={}:{}:force_original_aspect_ratio=increase," +
                        "crop={}:{},setsar=1,gblur=sigma={}[bg]",
                TARGET_WIDTH, TARGET_HEIGHT,
                TARGET_WIDTH, TARGET_HEIGHT,
                GAUSSIAN_BLUR_SIGMA);

        // 前景处理：缩放 + 绿幕抠像
        String fgFilter = StrUtil.format(
                "[1:v]scale={}:{},chromakey=green:similarity={}:blend={}[fg]",
                TARGET_WIDTH, TARGET_HEIGHT,
                CHROMAKEY_SIMILARITY, CHROMAKEY_BLEND);

        // 合成叠加：前景覆盖在背景上，居中对齐
        String overlayFilter = "[bg][fg]overlay=(W-w)/2:(H-h)/2:format=auto,format=yuv420p[out]";

        return bgFilter + ";" + fgFilter + ";" + overlayFilter;
    }

    /**
     * 读取 FFmpeg 进程输出（防止缓冲区写满导致进程卡死）
     *
     * @param process FFmpeg 进程
     * @return 进程输出日志
     */
    private String readProcessOutput(Process process) {
        var sb = new StringBuilder(4096);
        try (var reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                // 实时输出关键日志行
                if (line.contains("frame=") || line.contains("Error") || line.contains("error")) {
                    log.debug("FFmpeg: {}", line);
                }
            }
        } catch (Exception e) {
            log.warn("读取 FFmpeg 进程输出异常: {}", e.getMessage());
        }
        return sb.toString();
    }
}
