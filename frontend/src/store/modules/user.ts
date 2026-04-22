import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

interface UserInfo {
  id?: number
  username?: string
  nickname?: string
  avatar?: string
  roles?: string[]
  permissions?: string[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  async function login(loginData: { username: string; password: string }) {
    try {
      const res = await request.post('/auth/login', loginData) as any
      if (res.code === 200 || res.code === 0) {
        setToken(res.data.token)
        setUserInfo(res.data.userInfo)
        return true
      }
      return false
    } catch (error) {
      console.error('Login failed:', error)
      return false
    }
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      token.value = ''
      userInfo.value = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  async function getUserInfo() {
    try {
      const res = await request.get('/auth/userinfo') as any
      if (res.code === 200 || res.code === 0) {
        setUserInfo(res.data)
      }
    } catch (error) {
      console.error('Get user info failed:', error)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    login,
    logout,
    getUserInfo
  }
})