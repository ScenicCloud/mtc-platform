import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import router from '../router'
import type { ApiResult } from './types'
import { ErrorCode } from './types'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

// 请求拦截器：自动带 token
http.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：统一处理返回
http.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResult<any>
    // 成功
    if (data.code === 0) {
      return data
    }
    // 业务错误
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (error) => {
    const status = error.response?.status
    const data = error.response?.data as ApiResult<any> | undefined

    // 未登录或 token 过期 → 清 token 跳登录
    if (status === 401) {
      const code = data?.code
      if (code === ErrorCode.UNAUTHORIZED || code === ErrorCode.TOKEN_EXPIRED) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
        ElMessage.warning(data?.message || '登录已失效，请重新登录')
        return Promise.reject(error)
      }
      // 登录失败（2004）：留在当前页
      ElMessage.error(data?.message || '账号或密码错误')
      return Promise.reject(error)
    }

    // 网络错误
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }

    // 其他错误
    ElMessage.error(data?.message || `请求失败 (${status})`)
    return Promise.reject(error)
  }
)

export default http
