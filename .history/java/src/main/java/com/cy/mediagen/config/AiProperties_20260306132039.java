package com.cy.mediagen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 大模型服务配置属性类（通义千问 DashScope + TTS）
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 通义千问 DashScope 配置 */
    private DashScope dashscope = new DashScope();

    /** TTS 语音合成配置 */
    private Tts tts = new Tts();

    @Data
    public static class DashScope {
        /** API Key */
        private String apiKey;
        /** 模型名称（如 qwen-turbo） */
        private String model;
        /** API 基础地址 */
        private String baseUrl;
    }

    @Data
    public static class Tts {
        /** TTS API 基础地址 */
        private String baseUrl;
        /** 默认音色模型 ID */
        private String defaultVoiceId;
    }
}
