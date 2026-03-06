import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态管理 Store
 * 管理 JWT Token 和租户信息，持久化到 localStorage
 */
export const useUserStore = defineStore('user', () => {
  // ============= State =============

  const TOKEN_KEY = 'mg_token'

  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')

  // ============= Getters =============

  const isLoggedIn = computed(() => !!token.value)

  // ============= Actions =============

  /**
   * 设置 Token（登录成功后调用）
   */
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
  }

  /**
   * 清除 Token（登出 / 401 强制清除）
   */
  function clearToken() {
    token.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  /**
   * 登出
   */
  function logout() {
    clearToken()
  }

  return {
    token,
    isLoggedIn,
    setToken,
    clearToken,
    logout,
  }
})
