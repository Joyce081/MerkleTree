import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      // 代理所有以/user开头的请求
      '/user': {
        target: 'http://localhost:8080', // 后端API地址
        changeOrigin: true, // 改变请求源
        rewrite: (path) => path.replace(/^\/user/, '/user') // 可选，重写路径
      },
      // 如果需要代理其他API，可以继续添加
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  }
})