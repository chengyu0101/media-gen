<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">数字人视频生成平台</h1>
        <p class="login-subtitle">AI 驱动的短视频创作工作台</p>
      </div>

      <el-form
        :model="form"
        label-position="top"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="手机号 / 邮箱">
          <el-input
            v-model="form.username"
            placeholder="请输入手机号或邮箱"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const form = ref({
  username: "",
  password: "",
});

/**
 * 登录处理（MVP 占位：直接模拟 Token 写入，跳转工作台）
 */
async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning("请输入账号和密码");
    return;
  }

  loading.value = true;
  try {
    // MVP 阶段：模拟登录，实际接入时替换为后端登录接口
    await new Promise((resolve) => setTimeout(resolve, 600));
    userStore.setToken("mvp_mock_token_" + Date.now());
    ElMessage.success("登录成功");
    router.push("/");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(20px);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #8b8fa3;
  margin: 0;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #4a4a68;
}

.login-btn {
  width: 100%;
  margin-top: 12px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.login-btn:hover {
  opacity: 0.9;
}
</style>
