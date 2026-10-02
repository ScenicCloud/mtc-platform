// 统一返回格式
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId: string
}

// 错误码
export const ErrorCode = {
  UNAUTHORIZED: 2001,
  TOKEN_EXPIRED: 2002,
  BAD_CREDENTIALS: 2004,
} as const
