import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  appType: 'spa',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    outDir: 'dist', // 打包输出目录
    assetsDir: 'static' // 静态资源目录
  },
  server: {
    host: '0.0.0.0',
    port: 3001, // 前端服务端口
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端服务地址
        changeOrigin: true
      }
    }
  }
}) 