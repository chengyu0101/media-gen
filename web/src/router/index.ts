import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由配置
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Workspace',
    component: () => import('@/views/Workspace.vue'),
    meta: { title: '创作工作台' },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：设置页面标题
router.afterEach((to) => {
  const title = (to.meta?.title as string) || '数字人视频生成平台'
  document.title = `${title} - 数字人视频生成平台`
})

export default router
