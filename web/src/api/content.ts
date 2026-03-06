import request from '@/utils/request'

/**
 * AI 内容预处理 API 层
 * 对接后端 ContentController
 */

/**
 * 生成合规营销口播文案
 * @param intent 营销意图（如：推广水光针）
 * @param industry 行业类型（如：医美）
 * @returns 生成的合规文案文本
 */
export function generateScript(intent: string, industry: string): Promise<string> {
  return request.post('/api/v1/content/script', { intent, industry })
}

/**
 * TTS 语音合成
 * @param text 口播文案文本
 * @param voiceId 音色模型 ID（可选）
 * @returns 音频 OSS 链接
 */
export function generateVoice(text: string, voiceId?: string): Promise<string> {
  return request.post('/api/v1/content/voice', { text, voiceId })
}
