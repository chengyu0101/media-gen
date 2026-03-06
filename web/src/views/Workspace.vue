<template>
  <div class="workspace-container">
    <!-- ===== 顶部导航栏 ===== -->
    <header class="workspace-header">
      <div class="header-brand">
        <el-icon :size="28" color="#667eea"><VideoCamera /></el-icon>
        <h1 class="header-title">数字人视频生成平台</h1>
      </div>
      <div class="header-actions">
        <el-tag type="info" effect="plain" size="large">创作工作台</el-tag>
      </div>
    </header>

    <!-- ===== 主内容区 ===== -->
    <main class="workspace-main">
      <!-- 步骤条 -->
      <el-steps
        :active="activeStep"
        finish-status="success"
        align-center
        class="workspace-steps"
      >
        <el-step
          title="上传物料"
          description="人物底图 & 门店背景"
          :icon="UploadFilled"
        />
        <el-step
          title="文案与语音"
          description="AI 文案生成 & TTS 合成"
          :icon="Microphone"
        />
        <el-step
          title="渲染合成"
          description="视频合成 & 预览下载"
          :icon="Film"
        />
      </el-steps>

      <div class="step-content-wrapper">
        <!-- ==================== 步骤一：上传物料 ==================== -->
        <div v-show="activeStep === 0" class="step-content">
          <div class="step-section-title">
            <el-icon><Upload /></el-icon>
            <span>上传数字人物料</span>
          </div>

          <el-row :gutter="32">
            <el-col :span="12">
              <div class="upload-card">
                <h3 class="upload-card-title">
                  <el-icon color="#e6a23c"><Avatar /></el-icon>
                  人物高清底图
                </h3>
                <p class="upload-card-desc">
                  请上传正面半身照，分辨率建议 1080×1920
                </p>
                <UploadOss
                  asset-type="AVATAR_IMAGE"
                  accept="image/jpeg,image/png,image/webp"
                  placeholder="上传人物高清底图"
                  :max-size-mb="20"
                  @success="handleAvatarSuccess"
                />
              </div>
            </el-col>

            <el-col :span="12">
              <div class="upload-card">
                <h3 class="upload-card-title">
                  <el-icon color="#409eff"><Picture /></el-icon>
                  门店背景图
                </h3>
                <p class="upload-card-desc">
                  请上传门店实景照片，用于虚拟背景合成
                </p>
                <UploadOss
                  asset-type="BG_IMAGE"
                  accept="image/jpeg,image/png,image/webp"
                  placeholder="上传门店背景图"
                  :max-size-mb="20"
                  @success="handleBgSuccess"
                />
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- ==================== 步骤二：文案与语音生成 ==================== -->
        <div v-show="activeStep === 1" class="step-content">
          <div class="step-section-title">
            <el-icon><EditPen /></el-icon>
            <span>AI 文案与语音生成</span>
          </div>

          <el-form label-position="top" class="content-form">
            <!-- 营销意图 -->
            <el-form-item label="营销意图">
              <el-input
                v-model="scriptForm.intent"
                placeholder="请输入营销推广意图，例如：推广水光针、新店开业优惠"
                size="large"
                clearable
              >
                <template #append>
                  <el-button
                    type="primary"
                    :loading="scriptLoading"
                    :disabled="!scriptForm.intent.trim()"
                    @click="handleGenerateScript"
                  >
                    <el-icon class="mr-4"><MagicStick /></el-icon>
                    AI 帮我写
                  </el-button>
                </template>
              </el-input>
            </el-form-item>

            <!-- 行业类型 -->
            <el-form-item label="行业类型">
              <el-select
                v-model="scriptForm.industry"
                placeholder="选择行业类型"
                size="large"
                style="width: 100%"
              >
                <el-option label="医美" value="医美" />
                <el-option label="口腔" value="口腔" />
                <el-option label="美容美发" value="美容美发" />
                <el-option label="KTV" value="KTV" />
                <el-option label="餐饮" value="餐饮" />
                <el-option label="健身" value="健身" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>

            <!-- 生成的文案 -->
            <el-form-item label="口播文案">
              <el-input
                v-model="scriptForm.scriptText"
                type="textarea"
                :rows="6"
                placeholder="点击「AI 帮我写」自动生成合规文案，生成后可手动修改"
                resize="vertical"
              />
            </el-form-item>

            <el-divider content-position="left">语音合成</el-divider>

            <!-- 音色选择 -->
            <el-form-item label="选择音色">
              <el-select
                v-model="scriptForm.voiceId"
                placeholder="选择 TTS 音色"
                size="large"
                style="width: 100%"
              >
                <el-option label="知性女声（默认）" value="sambert-zhichu-v1" />
                <el-option label="温柔女声" value="sambert-zhimiao-v1" />
                <el-option label="磁性男声" value="sambert-zhida-v1" />
                <el-option label="活力男声" value="sambert-zhixiang-v1" />
                <el-option label="甜美女声" value="sambert-zhiyan-v1" />
              </el-select>
            </el-form-item>

            <!-- 语音合成按钮 & 播放器 -->
            <el-form-item>
              <el-button
                type="success"
                size="large"
                :loading="voiceLoading"
                :disabled="!scriptForm.scriptText.trim()"
                @click="handleGenerateVoice"
              >
                <el-icon><Headset /></el-icon>
                生成语音
              </el-button>

              <div v-if="audioUrl" class="audio-player-wrapper">
                <audio :src="audioUrl" controls class="audio-player" />
                <el-tag type="success" effect="plain" size="small">
                  <el-icon><CircleCheckFilled /></el-icon>
                  语音已就绪
                </el-tag>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- ==================== 步骤三：渲染与进度轮询 ==================== -->
        <div v-show="activeStep === 2" class="step-content">
          <div class="step-section-title">
            <el-icon><Film /></el-icon>
            <span>视频渲染合成</span>
          </div>

          <!-- 渲染前 —— 物料摘要 & 启动按钮 -->
          <div v-if="renderState === 'IDLE'" class="render-idle">
            <div class="render-summary">
              <h3 class="render-summary-title">创作摘要</h3>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="人物底图">
                  <el-tag type="success"
                    >已上传 (ID: {{ avatarAssetId }})</el-tag
                  >
                </el-descriptions-item>
                <el-descriptions-item label="门店背景">
                  <el-tag type="success">已上传 (ID: {{ bgAssetId }})</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="口播文案">
                  {{ scriptForm.scriptText.slice(0, 80) }}...
                </el-descriptions-item>
                <el-descriptions-item label="语音">
                  <el-tag :type="audioUrl ? 'success' : 'info'">
                    {{ audioUrl ? "已合成" : "未合成" }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <el-alert
              type="warning"
              show-icon
              :closable="false"
              title="提交后将扣除 10 算力点"
              description="视频合成将消耗算力资源，请确认物料信息无误后再提交。"
              style="margin: 20px 0"
            />

            <el-button
              type="primary"
              size="large"
              class="render-start-btn"
              :loading="renderSubmitting"
              @click="handleStartRender"
            >
              <el-icon><VideoCamera /></el-icon>
              开始合成视频
            </el-button>
          </div>

          <!-- 渲染中 —— 进度条 -->
          <div v-else-if="renderState === 'RENDERING'" class="render-progress">
            <div class="progress-visual">
              <el-progress
                type="dashboard"
                :percentage="renderProgress"
                :width="220"
                :stroke-width="12"
                :color="progressColors"
              >
                <template #default="{ percentage }">
                  <div class="progress-inner">
                    <span class="progress-number">{{ percentage }}%</span>
                    <span class="progress-label">{{ renderStatusText }}</span>
                  </div>
                </template>
              </el-progress>
            </div>
            <p class="progress-hint">视频正在渲染中，请勿关闭页面...</p>
          </div>

          <!-- 渲染成功 —— 视频预览 -->
          <div v-else-if="renderState === 'SUCCESS'" class="render-success">
            <div class="success-badge">
              <el-icon :size="48" color="#67c23a"
                ><CircleCheckFilled
              /></el-icon>
              <h2 class="success-title">视频合成完成！</h2>
            </div>

            <div class="video-preview-wrapper">
              <video
                :src="resultVideoUrl"
                controls
                class="video-player"
                preload="metadata"
              />
            </div>

            <div class="video-actions">
              <el-button
                type="primary"
                size="large"
                @click="handleDownloadVideo"
              >
                <el-icon><Download /></el-icon>
                下载视频
              </el-button>
              <el-button size="large" @click="handleRestart">
                <el-icon><RefreshRight /></el-icon>
                创建新视频
              </el-button>
            </div>
          </div>

          <!-- 渲染失败 -->
          <div v-else-if="renderState === 'FAILED'" class="render-failed">
            <el-result
              icon="error"
              title="渲染失败"
              :sub-title="renderErrorLog"
            >
              <template #extra>
                <el-button type="primary" @click="handleRetryRender"
                  >重新提交</el-button
                >
                <el-button @click="handleRestart">返回首页</el-button>
              </template>
            </el-result>
          </div>
        </div>
      </div>

      <!-- ===== 底部操作栏 ===== -->
      <div class="step-actions">
        <el-button
          v-if="activeStep > 0 && renderState === 'IDLE'"
          size="large"
          @click="activeStep--"
        >
          上一步
        </el-button>
        <el-button
          v-if="activeStep === 0"
          type="primary"
          size="large"
          :disabled="!canProceedStep1"
          @click="activeStep++"
        >
          下一步
        </el-button>
        <el-button
          v-if="activeStep === 1"
          type="primary"
          size="large"
          :disabled="!canProceedStep2"
          @click="activeStep++"
        >
          下一步：渲染合成
        </el-button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
import {
  UploadFilled,
  Microphone,
  Film,
  Upload,
  Avatar,
  Picture,
  EditPen,
  MagicStick,
  Headset,
  CircleCheckFilled,
  VideoCamera,
  Download,
  RefreshRight,
} from "@element-plus/icons-vue";
import UploadOss from "@/components/UploadOss.vue";
import { generateScript, generateVoice } from "@/api/content";
import { submitRenderTask, getTaskStatus } from "@/api/task";

// ======================== 步骤控制 ========================
const activeStep = ref(0);

// ======================== 步骤一：上传物料 ========================
const avatarAssetId = ref<number | null>(null);
const bgAssetId = ref<number | null>(null);

const canProceedStep1 = computed(
  () => avatarAssetId.value !== null && bgAssetId.value !== null,
);

function handleAvatarSuccess(assetId: number, _previewUrl: string) {
  avatarAssetId.value = assetId;
}

function handleBgSuccess(assetId: number, _previewUrl: string) {
  bgAssetId.value = assetId;
}

// ======================== 步骤二：文案与语音 ========================
const scriptForm = ref({
  intent: "",
  industry: "医美",
  scriptText: "",
  voiceId: "sambert-zhichu-v1",
});

const scriptLoading = ref(false);
const voiceLoading = ref(false);
const audioUrl = ref("");

const canProceedStep2 = computed(
  () => scriptForm.value.scriptText.trim().length > 0,
);

/**
 * AI 生成合规文案
 */
async function handleGenerateScript() {
  if (!scriptForm.value.intent.trim()) {
    ElMessage.warning("请先输入营销意图");
    return;
  }

  scriptLoading.value = true;
  try {
    const script = await generateScript(
      scriptForm.value.intent,
      scriptForm.value.industry,
    );
    scriptForm.value.scriptText = script;
    ElMessage.success("文案生成成功");
  } catch {
    // 错误已在 Axios 拦截器中处理
  } finally {
    scriptLoading.value = false;
  }
}

/**
 * TTS 语音合成
 */
async function handleGenerateVoice() {
  if (!scriptForm.value.scriptText.trim()) {
    ElMessage.warning("请先填写口播文案");
    return;
  }

  voiceLoading.value = true;
  try {
    const url = await generateVoice(
      scriptForm.value.scriptText,
      scriptForm.value.voiceId,
    );
    audioUrl.value = url;
    ElMessage.success("语音合成成功");
  } catch {
    // 错误已在 Axios 拦截器中处理
  } finally {
    voiceLoading.value = false;
  }
}

// ======================== 步骤三：渲染与轮询 ========================
type RenderState = "IDLE" | "RENDERING" | "SUCCESS" | "FAILED";

const renderState = ref<RenderState>("IDLE");
const renderSubmitting = ref(false);
const renderProgress = ref(0);
const renderStatusText = ref("排队中...");
const resultVideoUrl = ref("");
const renderErrorLog = ref("");

let pollingTimer: ReturnType<typeof setInterval> | null = null;

/** 进度条渐变色 */
const progressColors = [
  { color: "#909399", percentage: 20 },
  { color: "#e6a23c", percentage: 50 },
  { color: "#409eff", percentage: 80 },
  { color: "#67c23a", percentage: 100 },
];

/**
 * 提交渲染任务
 */
async function handleStartRender() {
  if (!avatarAssetId.value) {
    ElMessage.error("人像底图未上传");
    return;
  }

  renderSubmitting.value = true;
  try {
    const taskId = await submitRenderTask(
      avatarAssetId.value,
      bgAssetId.value,
      audioUrl.value || undefined,
    );

    ElMessage.success("渲染任务提交成功");
    renderState.value = "RENDERING";
    renderProgress.value = 0;
    renderStatusText.value = "排队中...";

    // 开启轮询
    startPolling(taskId);
  } catch {
    // 错误已在 Axios 拦截器中处理（例如 402 算力不足）
  } finally {
    renderSubmitting.value = false;
  }
}

/**
 * 启动轮询定时器 —— 每 3 秒查询一次任务状态
 *
 * 缓动进度算法：
 * - 进度 < 60%：每次 +随机 2~5
 * - 进度 60%~90%：每次 +随机 1~3
 * - 进度 > 90%：每次 +0.5，停在 99%
 */
function startPolling(taskId: number) {
  clearPolling();

  pollingTimer = setInterval(async () => {
    try {
      const status = await getTaskStatus(taskId);

      if (status.taskStatus === "SUCCESS") {
        // ======== 渲染成功 ========
        clearPolling();
        renderProgress.value = 100;
        renderStatusText.value = "合成完成";
        resultVideoUrl.value = status.resultVideoUrl || "";
        renderState.value = "SUCCESS";
        return;
      }

      if (status.taskStatus === "FAILED") {
        // ======== 渲染失败 ========
        clearPolling();
        renderErrorLog.value = status.errorLog || "未知错误，请联系客服";
        renderState.value = "FAILED";
        ElMessage.error("视频渲染失败：" + (status.errorLog || "未知错误"));
        return;
      }

      // ======== 进行中 —— 缓动递增进度 ========
      if (status.taskStatus === "PROCESSING") {
        renderStatusText.value = "渲染中...";
      } else {
        renderStatusText.value = "排队中...";
      }

      // 缓动进度算法
      if (renderProgress.value < 60) {
        renderProgress.value += Math.floor(Math.random() * 4) + 2;
      } else if (renderProgress.value < 90) {
        renderProgress.value += Math.floor(Math.random() * 3) + 1;
      } else if (renderProgress.value < 99) {
        renderProgress.value = Math.min(renderProgress.value + 0.5, 99);
      }
    } catch {
      // 轮询异常容错：不立即停止，等待下次重试
      console.warn("轮询任务状态异常，等待下次重试");
    }
  }, 3000);
}

/**
 * 清除轮询定时器
 */
function clearPolling() {
  if (pollingTimer !== null) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

/**
 * 下载视频
 */
function handleDownloadVideo() {
  if (resultVideoUrl.value) {
    const link = document.createElement("a");
    link.href = resultVideoUrl.value;
    link.download = `数字人视频_${Date.now()}.mp4`;
    link.target = "_blank";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}

/**
 * 重试渲染
 */
function handleRetryRender() {
  renderState.value = "IDLE";
  renderProgress.value = 0;
  renderErrorLog.value = "";
}

/**
 * 重新创建（回到步骤一）
 */
function handleRestart() {
  activeStep.value = 0;
  avatarAssetId.value = null;
  bgAssetId.value = null;
  scriptForm.value = {
    intent: "",
    industry: "医美",
    scriptText: "",
    voiceId: "sambert-zhichu-v1",
  };
  audioUrl.value = "";
  renderState.value = "IDLE";
  renderProgress.value = 0;
  resultVideoUrl.value = "";
  renderErrorLog.value = "";
}

// 组件卸载时清除定时器
onBeforeUnmount(() => {
  clearPolling();
});
</script>

<style scoped>
/* ===== 全局布局 ===== */
.workspace-container {
  min-height: 100vh;
  background: #f0f2f8;
  display: flex;
  flex-direction: column;
}

/* ===== 顶部导航 ===== */
.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 64px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* ===== 主内容 ===== */
.workspace-main {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 24px;
}

.workspace-steps {
  margin-bottom: 40px;
}

.step-content-wrapper {
  background: #fff;
  border-radius: 16px;
  padding: 36px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  min-height: 400px;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.step-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f2f8;
}

/* ===== 步骤一：上传物料 ===== */
.upload-card {
  padding: 20px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  transition: box-shadow 0.3s ease;
}

.upload-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.upload-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.upload-card-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 16px;
}

/* ===== 步骤二：文案与语音 ===== */
.content-form {
  max-width: 720px;
}

.mr-4 {
  margin-right: 4px;
}

.audio-player-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 16px;
}

.audio-player {
  height: 36px;
  border-radius: 8px;
}

/* ===== 步骤三：渲染 ===== */
.render-idle {
  max-width: 640px;
  margin: 0 auto;
}

.render-summary-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px;
  color: #303133;
}

.render-start-btn {
  width: 100%;
  height: 52px;
  font-size: 17px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.render-start-btn:hover {
  opacity: 0.9;
}

.render-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.progress-visual {
  margin-bottom: 24px;
}

.progress-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-number {
  font-size: 36px;
  font-weight: 700;
  color: #303133;
}

.progress-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.progress-hint {
  font-size: 14px;
  color: #909399;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.render-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.success-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.success-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.video-preview-wrapper {
  width: 100%;
  max-width: 640px;
  margin-bottom: 24px;
}

.video-player {
  width: 100%;
  border-radius: 12px;
  background: #000;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.video-actions {
  display: flex;
  gap: 16px;
}

.render-failed {
  padding: 40px 0;
}

/* ===== 底部操作栏 ===== */
.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}
</style>
