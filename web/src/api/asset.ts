import request from '@/utils/request'
import type { PreSignedUrlDTO } from '@/types/api'

/**
 * 资产管理 API 层
 * 对接后端 AssetController
 */

/**
 * 获取 OSS 预签名直传 URL
 * @param fileName 原始文件名
 * @param assetType 资产类型：AVATAR_IMAGE | BG_IMAGE | AUDIO
 */
export function getUploadUrl(fileName: string, assetType: string): Promise<PreSignedUrlDTO> {
  return request.get('/api/v1/assets/upload-url', {
    params: { fileName, assetType },
  })
}

/**
 * 保存资产元数据（OSS 直传完成后调用）
 * @param assetType 资产类型
 * @param objectKey OSS 对象 Key
 * @returns assetId 资产主键 ID
 */
export function saveAsset(assetType: string, objectKey: string): Promise<number> {
  return request.post('/api/v1/assets', { assetType, objectKey })
}
