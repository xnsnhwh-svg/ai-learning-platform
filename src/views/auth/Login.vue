<template>
  <div class="auth-page page-enter">
    <div class="auth-orb auth-orb-1"></div>
    <div class="auth-orb auth-orb-2"></div>
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">
          <svg width="40" height="40" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="url(#lg)"/>
            <path d="M14 7l5.5 10h-11z" fill="#fff" opacity="0.9"/>
            <circle cx="14" cy="19" r="3" fill="#fff" opacity="0.7"/>
            <defs><linearGradient id="lg" x1="0" y1="0" x2="28" y2="28"><stop stop-color="#5B7B8A"/><stop offset="1" stop-color="#C4704A"/></linearGradient></defs>
          </svg>
        </div>
        <h2>欢迎回来</h2>
        <p class="auth-desc">登录 AI 云学智训平台，继续你的学习之旅</p>
      </div>
      <el-form @submit.prevent="handleLogin" label-position="top" class="auth-form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="full-width press-scale" round>登录</el-button>
      </el-form>
      <p class="switch">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #1c1917 0%, #292524 40%, #1e1b4b 100%);
  position: relative;
  overflow: hidden;
}
.auth-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
}
.auth-orb-1 {
  width: 500px; height: 500px;
  background: radial-gradient(circle, rgba(91,123,138,0.15), transparent 70%);
  top: -150px; right: -100px;
}
.auth-orb-2 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(196,112,74,0.1), transparent 70%);
  bottom: -100px; left: -100px;
}
.auth-card {
  background: rgba(255,255,255,0.97);
  backdrop-filter: blur(20px);
  padding: var(--space-xl);
  border-radius: var(--radius-xl);
  width: 400px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  position: relative;
}
.auth-header { text-align: center; margin-bottom: var(--space-lg); }
.auth-logo { margin-bottom: var(--space-md); }
.auth-header h2 { font-size: var(--fs-2xl); color: var(--text-primary); margin: 0 0 var(--space-xs); font-weight: var(--fw-bold); }
.auth-desc { color: var(--text-secondary); font-size: var(--fs-base); margin: 0; }
.auth-form { margin-bottom: var(--space-md); }
.full-width { width: 100%; height: 42px; font-size: var(--fs-base); }
.switch { text-align: center; color: var(--text-secondary); font-size: var(--fs-base); }
.switch a { color: var(--primary); text-decoration: none; font-weight: var(--fw-medium); }
.switch a:hover { text-decoration: underline; }
</style>
