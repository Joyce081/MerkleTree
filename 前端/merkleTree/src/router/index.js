import { createRouter, createWebHistory } from 'vue-router'
import LoginRegister from '@/views/LoginRegister.vue'
import Home from '@/views/Home.vue'
import Personal from '@/views/Personal.vue'
import store from '@/store'

const routes = [
  {
    path: '/',
    name: 'LoginRegister',
    component: LoginRegister,
    meta: {
      requiresAuth: false // 不需要登录即可访问
    }
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/personal',
    name: 'Personal',
    component: Personal,
    meta: {
      requiresAuth: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫 - 检查用户是否已登录
router.beforeEach((to, from, next) => {
  // 初始化认证状态
  store.dispatch('initializeAuth');

  if (to.meta.requiresAuth && !store.state.auth.isAuthenticated) {
    // 如果需要登录但用户未登录，则重定向到登录页面
    next('/');
  } else {
    // 否则继续导航
    next();
  }
});

export default router