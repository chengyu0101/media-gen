import request from '@/utils/request'
import type { VideoTaskStatusDTO } from '@/types/api'

/**
 * 视频渲染任务 API 层
 * 对接后端 VideoTaskController
 */

/**
 * 提交视频渲染任务
 * @param sourceAssetId 人像底图资产 ID
 * @param bgAssetId 门店背景图资产 ID（可选）
 * @param audioUrl 合成语音 OSS 链接（可选）
 * @returns taskId 任务 ID
 */
export function submitRenderTask(
  sourceAssetId: number,
  bgAssetId?: number | null,
  audioUrl?: string,
): Promise<number> {
  return request.post('/api/v1/tasks/render', { sourceAssetId, bgAssetId, audioUrl })
}

/**
 * 查询任务状态
 * @param taskId 任务 ID
 * @returns 视频任务状态 DTO
 */
export function getTaskStatus(taskId: number): Promise<VideoTaskStatusDTO> {
  return request.get(`/api/v1/tasks/${taskId}`)
}
