<template>
  <div class="navbar">
    <div class="navbar-inner">
      <div class="logo" @click="$router.push('/home')">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="url(#logo-grad)"/>
            <path d="M14 7l5.5 10h-11z" fill="#fff" opacity="0.9"/>
            <circle cx="14" cy="19" r="3" fill="#fff" opacity="0.7"/>
                <defs><linearGradient id="logo-grad" x1="0" y1="0" x2="28" y2="28"><stop stop-color="#5B7B8A"/><stop offset="1" stop-color="#C4704A"/></linearGradient></defs>
          </svg>
        </div>
        <span class="title">AI 云学智训</span>
      </div>
      <div class="nav-menu">
        <div
          v-for="item in menuItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: currentPath.startsWith(item.path) }"
          @click="$router.push(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <div class="nav-indicator" v-if="currentPath.startsWith(item.path)"></div>
        </div>
      </div>
      <div class="nav-right">
        <el-dropdown v-if="auth.isLoggedIn" @command="handleCommand">
          <span class="user-info">
            <div class="user-avatar">{{ auth.displayName?.charAt(0)?.toUpperCase() || 'U' }}</div>
            <span class="user-name">{{ auth.displayName }}</span>
            <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>用户画像
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-else round @click="$router.push('/login')">登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const currentPath = computed(() => route.path)

const menuItems = [
  { path: '/home', label: '首页', icon: 'HomeFilled' },
  { path: '/chat', label: '智能对话', icon: 'ChatDotRound' },
  { path: '/paths', label: '学习路径', icon: 'Guide' },
  { path: '/knowledge', label: '知识图谱', icon: 'Connection' },
  { path: '/profile', label: '用户画像', icon: 'User' },
  { path: '/videos', label: '视频学习', icon: 'VideoCamera' }
]

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  justify-content: center;
  padding: 0 var(--space-lg);
  height: var(--navbar-height);
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}
.navbar-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: var(--content-max);
}
.logo {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
  user-select: none;
}
.logo-icon { display: flex; align-items: center; }
.title { font-size: var(--fs-xl); font-weight: var(--fw-bold); background: var(--primary-gradient); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.nav-menu { display: flex; align-items: center; gap: var(--space-xs); }
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm) 14px;
  border-radius: var(--radius-md);
  color: var(--text-regular);
  cursor: pointer;
  font-size: var(--fs-base);
  transition: all var(--t-fast);
  white-space: nowrap;
}
.nav-item:hover { background: var(--bg-hover); color: var(--primary); }
.nav-item.active { color: var(--primary); font-weight: var(--fw-medium); }
.nav-item .el-icon { font-size: 16px; }
.nav-indicator {
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  border-radius: var(--radius-full);
  background: var(--primary-gradient);
}
.nav-right { display: flex; align-items: center; }
.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
  color: var(--text-regular);
  font-size: var(--fs-base);
  padding: 4px 8px 4px 4px;
  border-radius: var(--radius-full);
  transition: all var(--t-fast);
}
.user-info:hover { background: var(--bg-hover); }
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
}
.user-name { color: var(--text-primary); font-weight: var(--fw-medium); font-size: var(--fs-base); }
.dropdown-arrow { font-size: 12px; color: var(--text-secondary); transition: transform var(--t-fast); }
.user-info:hover .dropdown-arrow { transform: rotate(180deg); }
</style>
