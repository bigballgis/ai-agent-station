import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as userApi from '@/api/user'
import type { UserInfo } from '@/types/user'
import { setRefreshToken as storeRefreshToken, clearAuth as clearAllAuth } from '@/utils/authStorage'
import { logger } from '@/utils/logger'
import { requireStoreReady } from '../utils'

export const useUserStore = defineStore('user', () => {
  requireStoreReady('user')

  // State
  const token = ref<string>(localStorage.getItem('token') || sessionStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>(JSON.parse(localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo') || '{}'))

  // Getters
  const isLoggedIn = computed<boolean>(() => !!token.value)

  // Actions
  function setToken(newToken: string, remember: boolean = false): void {
    token.value = newToken
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('token', newToken)
    // 同时清除另一个存储中的token
    const otherStorage = remember ? sessionStorage : localStorage
    otherStorage.removeItem('token')
  }

  function setUserInfo(info: UserInfo, remember: boolean = false): void {
    userInfo.value = info
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('userInfo', JSON.stringify(info))
    const otherStorage = remember ? sessionStorage : localStorage
    otherStorage.removeItem('userInfo')
  }

  async function login(loginData: { username: string; password: string; remember?: boolean; captchaId?: string; captchaAnswer?: string }): Promise<boolean> {
    try {
      const res = await userApi.login(loginData)
      if (res.code === 200 || res.code === 0) {
        const remember = loginData.remember ?? false
        setToken(res.data.token, remember)
        setUserInfo(res.data.userInfo, remember)
        if (res.data.refreshToken) {
          storeRefreshToken(res.data.refreshToken, remember)
        }
        return true
      }
      return false
    } catch (error) {
      logger.debug('Login failed:', error)
      return false
    }
  }

  async function logout(): Promise<void> {
    try {
      await userApi.logout()
    } catch (error) {
      logger.debug('Logout error:', error)
    } finally {
      clearAllAuth()
      token.value = ''
      userInfo.value = {} as UserInfo
    }
  }

  async function getUserInfo(): Promise<void> {
    try {
      const res = await userApi.getUserInfo()
      if (res.code === 200 || res.code === 0) {
        setUserInfo(res.data)
      }
    } catch (error) {
      logger.debug('Get user info failed:', error)
    }
  }

  function $reset(): void {
    token.value = ''
    userInfo.value = {} as UserInfo
  }

  return {
    // State
    token,
    userInfo,
    // Getters
    isLoggedIn,
    // Actions
    setToken,
    setUserInfo,
    login,
    logout,
    getUserInfo,
    $reset,
  }
})
