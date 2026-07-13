import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/apple-hig.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 鍒涘缓Vue搴旂敤瀹炰緥
const app = createApp(App)

// 娉ㄥ唽Element Plus鍥炬爣
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 浣跨敤鎻掍欢
app.use(createPinia()) // 鐘舵€佺鐞?
app.use(router) // 璺敱
app.use(ElementPlus) // UI缁勪欢搴?

// 鎸傝浇搴旂敤
app.mount('#app') 
