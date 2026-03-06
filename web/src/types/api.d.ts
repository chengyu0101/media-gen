/**
 * 后端统一返回体 Result<T> 类型定义
 * 与 com.cy.mediagen.dto.Result 完全对齐
 */
export interface Result<T = unknown> {
  /** 状态码：200=成功，402=算力不足，429=限流，401=未授权 */
  code: number
  /** 提示信息 */
  message: string
  /** 数据载荷 */
  data: T
  /** 时间戳 */
  timestamp: string
}

// ============= 资产管理 DTO =============

/** OSS 预签名上传结果 */
export interface PreSignedUrlDTO {
  /** 预签名上传 URL（客户端 PUT 直传阿里云 OSS） */
  uploadUrl: string
  /** OSS 对象唯一标识 Key */
  objectKey: string
}

/** 资产元数据保存请求 */
export interface AssetSaveRequest {
  /** 资产类型：AVATAR_IMAGE | BG_IMAGE | AUDIO */
  assetType: string
  /** OSS 对象 Key */
  objectKey: string
}

// ============= 内容预处理 DTO =============

/** 合规文案生成请求 */
export interface ScriptGenerateRequest {
  /** 营销意图，例如：推广水光针 */
  intent: string
  /** 行业类型，例如：医美 */
  industry: string
}

/** TTS 语音合成请求 */
export interface VoiceGenerateRequest {
  /** 口播文案文本 */
  text: string
  /** 音色模型 ID（可选） */
  voiceId?: string
}

// ============= 视频任务 DTO =============

/** 渲染任务提交请求 */
export interface RenderTaskRequest {
  /** 人像底图资产 ID */
  sourceAssetId: number
  /** 门店背景图资产 ID（可选） */
  bgAssetId?: number | null
  /** 合成好的语音 OSS 链接 */
  audioUrl?: string
}

/** 视频任务状态响应 */
export interface VideoTaskStatusDTO {
  /** 任务 ID */
  taskId: number
  /** 任务状态：PENDING | PROCESSING | SUCCESS | FAILED */
  taskStatus: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED'
  /** 最终成片视频链接（SUCCESS 时有值） */
  resultVideoUrl?: string | null
  /** 失败原因（FAILED 时有值） */
  errorLog?: string | null
}
