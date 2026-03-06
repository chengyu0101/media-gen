package com.cy.mediagen.service;

/**
 * AI 内容生成服务接口（防腐层 ACL）
 * <p>
 * 隔离第三方大模型 API 的变动，防止污染业务代码。
 * 所有 AI 能力的调用均通过此接口暴露。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
public interface AiContentService {

    /**
     * 生成合规营销口播文案
     * <p>
     * 调用大模型生成符合中国《广告法》及医疗美容广告监管规定的口播文案。
     * 严禁生成含有极限词、虚假承诺的内容。
     * </p>
     *
     * @param intent   营销意图（如"推广水光针"）
     * @param industry 行业类型（如"医美"）
     * @return 生成的口播文案
     */
    String generateCompliantScript(String intent, String industry);

    /**
     * TTS 语音合成
     * <p>
     * 调用第三方 TTS API 将口播文案转为音频文件。
     * </p>
     *
     * @param text    口播文案文本
     * @param voiceId 指定的音色模型 ID（为空则使用默认音色）
     * @return 生成的音频文件本地临时路径
     */
    String generateSpeech(String text, String voiceId);
}
