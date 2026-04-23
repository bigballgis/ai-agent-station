import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as userApi from '@/api/user'
import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || sessionStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>(JSON.parse(localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo') || '{}'))

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string, remember: boolean = false) {
    token.value = newToken
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('token', newToken)
    // 同时清除另一个存储中的token
    const otherStorage = remember ? sessionStorage : localStorage
    otherStorage.removeItem('token')
  }

  function setUserInfo(info: UserInfo, remember: boolean = false) {
    userInfo.value = info
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('userInfo', JSON.stringify(info))
    const otherStorage = remember ? sessionStorage : localStorage
    otherStorage.removeItem('userInfo')
  }

  async function login(loginData: { username: string; password: string; remember?: boolean; captchaId?: string; captchaAnswer?: string }) {
    try {
      const res = await userApi.login(loginData)
      if (res.code === 200 || res.code === 0) {
        const remember = loginData.remember ?? false
        setToken(res.data.token, remember)
        setUserInfo(res.data.userInfo, remember)
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
      await userApi.logout()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      token.value = ''
      userInfo.value = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('userInfo')
    }
  }

  async function getUserInfo() {
    try {
      const res = await userApi.getUserInfo()
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
