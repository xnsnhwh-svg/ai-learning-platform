import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(parseInt(localStorage.getItem('userId') || '0'))
  const username = ref(localStorage.getItem('username') || '')
  const realName = ref(localStorage.getItem('realName') || '')

  const isLoggedIn = computed(() => !!token.value)
  const displayName = computed(() => realName.value || username.value || '游客')

  async function login(loginData) {
    const res = await request.post('/api/auth/login', loginData)
    const data = res.data
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('realName', data.realName || '')
    return data
  }

  async function register(regData) {
    const res = await request.post('/api/auth/register', regData)
    const data = res.data
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('realName', data.realName || '')
    return data
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    realName.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
  }

  return { token, userId, username, realName, isLoggedIn, displayName, login, register, logout }
})
