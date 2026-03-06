package com.cy.mediagen.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.cy.mediagen.context.TenantContextHolder;
import com.cy.mediagen.dto.Result;
import com.cy.mediagen.dto.ScriptGenerateRequest;
import com.cy.mediagen.dto.VoiceGenerateRequest;
import com.cy.mediagen.service.AiContentService;
import com.cy.mediagen.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 内容预处理接口（文案生成 + 语音合成）
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/content")
@Tag(name = "内容预处理", description = "AI 合规文案生成与 TTS 语音合成接口")
public class ContentController {

    private final AiContentService aiContentService;
    private final OssService ossService;

    /** 语音文件 OSS 目录模板 */
    private static final String AUDIO_OSS_DIR = "tenant_{}/audio/";

    /**
     * 生成合规营销口播文案
     */
    @PostMapping("/script")
    @Operation(summary = "生成合规文案", description = "调用大模型生成符合广告法的医美/本地生活口播文案")
    public Result<String> generateScript(@Valid @RequestBody ScriptGenerateRequest request) {
        Long tenantId = requireTenantId();
        log.info("生成合规文案请求，tenantId: {}，意图: {}，行业: {}",
                tenantId, request.getIntent(), request.getIndustry());

        String script = aiContentService.generateCompliantScript(request.getIntent(), request.getIndustry());
        return Result.success("文案生成成功", script);
    }

    /**
     * TTS 语音合成
     */
    @PostMapping("/voice")
    @Operation(summary = "TTS 语音合成", description = "将确认后的文案转为语音，返回音频 OSS 链接")
    public Result<String> generateVoice(@Valid @RequestBody VoiceGenerateRequest request) {
        Long tenantId = requireTenantId();
        log.info("TTS 语音合成请求，tenantId: {}，文本长度: {}，音色: {}",
                tenantId, request.getText().length(), request.getVoiceId());

        // 调用 TTS 生成本地临时音频文件
        String localAudioPath = aiContentService.generateSpeech(request.getText(), request.getVoiceId());

        // 上传音频到 OSS
        String objectKey = StrUtil.format(AUDIO_OSS_DIR, tenantId) + IdUtil.fastSimpleUUID() + ".mp3";
        String audioUrl = ossService.uploadFile(localAudioPath, objectKey);

        log.info("TTS 语音合成完成，tenantId: {}，audioUrl: {}", tenantId, audioUrl);
        return Result.success("语音合成成功", audioUrl);
    }

    /**
     * 从 TenantContextHolder 获取当前租户 ID
     */
    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("无法识别当前租户，请确认请求头中包含租户信息");
        }
        return tenantId;
    }
}
