import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '../api/http'
import type { ApiResult } from '../api/types'

const TOKEN_KEY = 'mtc_token'
const USER_KEY = 'mtc_user'

interface UserInfo {
  id: number
  username: string
  roles: string[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserInfo | null>(() => {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  })

  const isLoggedIn = computed(() => !!token.value)

  const setToken = (t: string) => {
    token.value = t
    localStorage.setItem(TOKEN_KEY, t)
  }

  const setUser = (u: UserInfo) => {
    user.value = u
    localStorage.setItem(USER_KEY, JSON.stringify(u))
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  const login = async (username: string, password: string) => {
    const res = await http.post<any, ApiResult<{
      token: string
      tokenType: string
      expiresIn: number
      user: UserInfo
    }>>('/auth/login', { username, password })
    setToken(res.data.token)
    setUser(res.data.user)
    return res.data
  }

  const fetchMe = async () => {
    const res = await http.get<any, ApiResult<UserInfo>>('/auth/me')
    setUser(res.data)
    return res.data
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    fetchMe,
    logout,
  }
})
