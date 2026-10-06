import { createSseRequest, type SseEvent } from './sse'
import http from './http'
import type { ApiResult } from './types'
import type { PageResult } from './testCase'

// ========== 类型定义 ==========

export interface TestRun {
  id: number
  projectId: number
  scriptId?: number
  scriptName: string
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'
  totalTests: number
  passedTests: number
  failedTests: number
  skippedTests: number
  durationMs?: number
  errorMessage?: string
  baseUrl?: string
  createdBy?: number
  createdAt: string
  startedAt?: string
  finishedAt?: string
  updatedAt: string
}

export interface TestRunResult {
  id: number
  runId: number
  title: string
  file?: string
  status: 'passed' | 'failed' | 'skipped' | 'timedOut' | string
  durationMs?: number
  errorMessage?: string
  screenshotPath?: string
  startedAt?: string
  finishedAt?: string
  createdAt: string
}

export interface ExecuteContentRequest {
  projectId: number
  content: string
  name: string
  baseUrl?: string
}

export interface SseStreamResult {
  abort: () => void
  onEvent: (callback: (event: SseEvent) => void) => void
  onError: (callback: (error: string) => void) => void
  onDone: (callback: () => void) => void
}

// ========== SSE 执行封装 ==========

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
 * 直接执行脚本内容（SSE 流式输出执行日志）
 */
export const executeScriptContent = (
  data: ExecuteContentRequest,
  token?: string
): SseStreamResult => {
  return buildSseRequest('/api/v1/test-runs/execute-content', data, token)
}

/**
 * 执行已保存的测试脚本（SSE 流式输出）
 */
export const executeSavedScript = (
  scriptId: number,
  baseUrl?: string,
  token?: string
): SseStreamResult => {
  const url = baseUrl
    ? `/api/v1/test-scripts/${scriptId}/execute?baseUrl=${encodeURIComponent(baseUrl)}`
    : `/api/v1/test-scripts/${scriptId}/execute`
  return buildSseRequest(url, {}, token)
}

/**
 * 取消执行
 */
export const cancelRun = (runId: number) => {
  return http.post<any, ApiResult<null>>(`/test-runs/${runId}/cancel`)
}

/**
 * 分页查询项目下的运行记录
 */
export const getTestRuns = (
  projectId: number,
  params?: { page?: number; pageSize?: number }
) => {
  return http.get<any, ApiResult<PageResult<TestRun>>>(
    `/projects/${projectId}/test-runs`,
    { params }
  )
}

/**
 * 获取运行详情
 */
export const getTestRun = (runId: number) => {
  return http.get<any, ApiResult<TestRun>>(`/test-runs/${runId}`)
}

/**
 * 获取运行的用例结果列表
 */
export const getRunResults = (runId: number) => {
  return http.get<any, ApiResult<TestRunResult[]>>(`/test-runs/${runId}/results`)
}

/**
 * 获取单条用例结果详情
 */
export const getRunResult = (resultId: number) => {
  return http.get<any, ApiResult<TestRunResult>>(`/test-run-results/${resultId}`)
}
