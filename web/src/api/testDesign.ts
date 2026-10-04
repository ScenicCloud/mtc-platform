import { createSseRequest, type SseEvent } from './sse'
import type { TestCase } from './testCase'

// ========== 类型定义 ==========

export interface GenerateTestCasesRequest {
  projectId: number
  requirement: string
  docIds?: number[]
  context?: string
}

export interface GenerateTestScriptsRequest {
  projectId: number
  testCases: any[]
  module?: string
  baseUrl?: string
}

export interface GenerateTestDataRequest {
  projectId: number
  testCases: any[]
  module?: string
}

export interface SseStreamResult {
  abort: () => void
  onEvent: (callback: (event: SseEvent) => void) => void
  onError: (callback: (error: string) => void) => void
  onDone: (callback: () => void) => void
}

// ========== 内部封装 ==========

function buildSseRequest(
  url: string,
  body: any,
  token?: string
): SseStreamResult {
  let eventCb: ((event: SseEvent) => void) | null = null
  let errorCb: ((error: string) => void) | null = null
  let doneCb: (() => void) | null = null

  const { abort } = createSseRequest({
    url,
    method: 'POST',
    body,
    token,
    onEvent: (event: SseEvent) => {
      eventCb?.(event)
    },
    onError: (msg: string) => {
      errorCb?.(msg)
    },
    onDone: () => {
      doneCb?.()
    },
  })

  return {
    abort,
    onEvent: (cb) => {
      eventCb = cb
    },
    onError: (cb) => {
      errorCb = cb
    },
    onDone: (cb) => {
      doneCb = cb
    },
  }
}

// ========== API 方法 ==========

/**
 * 流式生成测试用例
 */
export const generateTestCases = (
  data: GenerateTestCasesRequest,
  token?: string
): SseStreamResult => {
  return buildSseRequest('/api/v1/test-design/test-cases/generate', data, token)
}

/**
 * 流式生成测试脚本
 */
export const generateTestScripts = (
  data: GenerateTestScriptsRequest,
  token?: string
): SseStreamResult => {
  return buildSseRequest('/api/v1/test-design/test-scripts/generate', data, token)
}

/**
 * 流式生成测试数据
 */
export const generateTestData = (
  data: GenerateTestDataRequest,
  token?: string
): SseStreamResult => {
  return buildSseRequest('/api/v1/test-design/test-data/generate', data, token)
}
