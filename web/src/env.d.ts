/// <reference types="vite/client" />

/**
 * Vue 单文件组件类型声明
 * 解决 TypeScript 无法识别 .vue 模块的问题
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
