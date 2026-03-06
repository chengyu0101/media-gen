import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'
import router from '@/router'

/**
 * Axios 实例 — 核心 HTTP 网络层
 *
 * 请求拦截器：自动注入 JWT Token
 * 响应拦截器：解包 Result<T>，对齐后端 GlobalExceptionHandler
 */
const service: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 30000,
})

// ======================== 请求拦截器 ========================
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('mg_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// ======================== 响应拦截器 ========================
service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data

    // 业务成功：直接解包返回 data
    if (res.code === 200) {
      return res.data as any
    }

    // -------- 业务异常静默处理 --------

    // 算力余额不足
    if (res.code === 402) {
      ElMessage.error('算力余额不足，请充值')
      return Promise.reject(new Error('算力余额不足'))
    }

    // 接口限流
    if (res.code === 429) {
      ElMessage.error('操作频繁，请稍后再试')
      return Promise.reject(new Error('操作频繁'))
    }

    // 登录失效
    if (res.code === 401) {
      localStorage.removeItem('mg_token')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(new Error('未授权'))
    }

    // 其他业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },

  // -------- HTTP 层错误（非 2xx 状态码） --------
  (error) => {
    const status = error.response?.status
    const data = error.response?.data as Result | undefined

    if (status === 401) {
      localStorage.removeItem('mg_token')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else if (status === 402) {
      ElMessage.error(data?.message || '算力余额不足，请充值')
    } else if (status === 429) {
      ElMessage.error(data?.message || '操作频繁，请稍后再试')
    } else if (status === 400) {
      ElMessage.error(data?.message || '请求参数有误')
    } else if (status === 403) {
      ElMessage.error(data?.message || '无权访问')
    } else if (status === 500) {
      ElMessage.error('系统繁忙，请稍后重试')
    } else {
      ElMessage.error(error.message || '网络错误')
    }

    return Promise.reject(error)
  },
)

export default service
