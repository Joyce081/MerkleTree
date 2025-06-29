import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

app.use(store)
// 初始化认证状态
store.dispatch('initializeAuth')
app.use(router)
app.use(ElementPlus)

app.mount('#app')