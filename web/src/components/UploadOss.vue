<template>
  <div class="upload-oss-wrapper">
    <el-upload
      ref="uploadRef"
      class="oss-uploader"
      :accept="accept"
      :show-file-list="false"
      :http-request="handleCustomUpload"
      :before-upload="handleBeforeUpload"
      drag
    >
      <!-- 上传中状态 -->
      <div v-if="uploading" class="upload-loading">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p class="upload-loading-text">{{ uploadStepText }}</p>
      </div>

      <!-- 上传完成 —— 图片预览 -->
      <div v-else-if="previewUrl && isImageType" class="upload-preview">
        <img :src="previewUrl" alt="预览" class="preview-image" />
        <div class="preview-overlay">
          <el-icon :size="20"><RefreshRight /></el-icon>
          <span>重新上传</span>
        </div>
      </div>

      <!-- 上传完成 —— 音频预览 -->
      <div
        v-else-if="previewUrl && !isImageType"
        class="upload-preview audio-preview"
      >
        <el-icon :size="36" color="var(--el-color-success)"
          ><CircleCheckFilled
        /></el-icon>
        <p class="upload-success-text">上传完成</p>
      </div>

      <!-- 默认空状态 -->
      <div v-else class="upload-placeholder">
        <el-icon :size="40"><UploadFilled /></el-icon>
        <p class="upload-placeholder-text">{{ placeholder }}</p>
        <p class="upload-hint-text">点击或拖拽文件到此处上传</p>
      </div>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { ElMessage } from "element-plus";
import {
  UploadFilled,
  Loading,
  RefreshRight,
  CircleCheckFilled,
} from "@element-plus/icons-vue";
import type { UploadRequestOptions } from "element-plus";
import { getUploadUrl, saveAsset } from "@/api/asset";

// ============= Props =============
const props = withDefaults(
  defineProps<{
    /** 资产类型：AVATAR_IMAGE | BG_IMAGE | AUDIO */
    assetType: string;
    /** 文件类型限制 */
    accept?: string;
    /** 占位提示文案 */
    placeholder?: string;
    /** 最大文件大小（MB） */
    maxSizeMb?: number;
  }>(),
  {
    accept: "image/*",
    placeholder: "上传文件",
    maxSizeMb: 20,
  },
);

// ============= Emits =============
const emit = defineEmits<{
  /** 上传全流程成功后触发 */
  success: [assetId: number, previewUrl: string];
}>();

// ============= State =============
const uploading = ref(false);
const uploadStep = ref(0); // 0=空闲, 1=获取签名, 2=上传 OSS, 3=资产入库
const previewUrl = ref("");

const isImageType = computed(
  () => props.assetType === "AVATAR_IMAGE" || props.assetType === "BG_IMAGE",
);

const uploadStepText = computed(() => {
  const steps = ["", "获取上传签名...", "上传至云存储...", "保存资产信息..."];
  return steps[uploadStep.value] || "处理中...";
});

// ============= Handlers =============

/**
 * 上传前校验文件大小
 */
function handleBeforeUpload(file: File): boolean {
  const maxBytes = props.maxSizeMb * 1024 * 1024;
  if (file.size > maxBytes) {
    ElMessage.error(`文件大小不能超过 ${props.maxSizeMb}MB`);
    return false;
  }
  return true;
}

/**
 * 自定义上传行为 —— 三步管线
 * Step 1: 获取预签名 URL
 * Step 2: 裸传阿里云 OSS（不带 Authorization）
 * Step 3: 资产元数据入库
 */
async function handleCustomUpload(options: UploadRequestOptions) {
  const file = options.file;
  uploading.value = true;

  try {
    // ======== Step 1: 获取预签名上传 URL ========
    uploadStep.value = 1;
    const { uploadUrl, objectKey } = await getUploadUrl(
      file.name,
      props.assetType,
    );

    // ======== Step 2: 裸传 OSS（不带 Token，跨域直传阿里云） ========
    uploadStep.value = 2;
    const putResponse = await fetch(uploadUrl, {
      method: "PUT",
      headers: {
        "Content-Type": file.type || "application/octet-stream",
      },
      body: file,
    });

    if (!putResponse.ok) {
      throw new Error(`OSS 上传失败，HTTP 状态：${putResponse.status}`);
    }

    // ======== Step 3: 资产元数据入库 ========
    uploadStep.value = 3;
    const assetId = await saveAsset(props.assetType, objectKey);

    // ======== 上传成功 ========
    // 本地预览使用 URL.createObjectURL 避免依赖 OSS 链接
    const localPreviewUrl = URL.createObjectURL(file);
    previewUrl.value = localPreviewUrl;

    ElMessage.success("上传成功");
    emit("success", assetId, localPreviewUrl);
  } catch (error: any) {
    ElMessage.error(error?.message || "上传失败，请重试");
    console.error("上传流程异常：", error);
  } finally {
    uploading.value = false;
    uploadStep.value = 0;
  }
}
</script>

<style scoped>
.upload-oss-wrapper {
  width: 100%;
}

.oss-uploader :deep(.el-upload) {
  width: 100%;
}

.oss-uploader :deep(.el-upload-dragger) {
  width: 100%;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 2px dashed var(--el-border-color-lighter);
  transition: all 0.3s ease;
  background: var(--el-fill-color-lighter);
}

.oss-uploader :deep(.el-upload-dragger:hover) {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.upload-placeholder {
  text-align: center;
  color: var(--el-text-color-secondary);
}

.upload-placeholder-text {
  margin: 8px 0 4px;
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.upload-hint-text {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.upload-loading {
  text-align: center;
  color: var(--el-color-primary);
}

.upload-loading-text {
  margin-top: 8px;
  font-size: 13px;
}

.upload-preview {
  position: relative;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.preview-image {
  width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 8px;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.25s ease;
  border-radius: 8px;
}

.upload-preview:hover .preview-overlay {
  opacity: 1;
}

.audio-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.upload-success-text {
  font-size: 13px;
  color: var(--el-color-success);
}
</style>
