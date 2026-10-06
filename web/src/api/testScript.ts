import http from './http'
import type { ApiResult } from './types'
import type { PageResult } from './testCase'

// ========== 类型定义 ==========

export interface TestScript {
  id: number
  projectId: number
  testCaseId?: number
  name: string
  framework: string
  language: string
  content: string
  status: 'draft' | 'generating' | 'ready' | 'failed'
  createdBy?: number
  createdAt: string
  updatedAt: string
}

export interface TestScriptCreateDTO {
  projectId: number
  testCaseId?: number
  name: string
  framework?: string
  language?: string
  content: string
  status?: string
}

export interface TestScriptUpdateDTO {
  name?: string
  framework?: string
  language?: string
  content?: string
  status?: string
  testCaseId?: number
}

export interface TestScriptPageParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

// ========== API 方法 ==========

/**
 * 分页查询项目下的测试脚本
 */
export const getTestScripts = (projectId: number, params?: TestScriptPageParams) => {
  return http.get<any, ApiResult<PageResult<TestScript>>>(
    `/projects/${projectId}/test-scripts`,
    { params }
  )
}

/**
 * 获取测试脚本详情
 */
export const getTestScript = (id: number) => {
  return http.get<any, ApiResult<TestScript>>(`/test-scripts/${id}`)
}

/**
 * 创建测试脚本
 */
export const createTestScript = (data: TestScriptCreateDTO) => {
  return http.post<any, ApiResult<TestScript>>('/test-scripts', data)
}

/**
 * 更新测试脚本
 */
export const updateTestScript = (id: number, data: TestScriptUpdateDTO) => {
  return http.put<any, ApiResult<TestScript>>(`/test-scripts/${id}`, data)
}

/**
 * 删除测试脚本
 */
export const deleteTestScript = (id: number) => {
  return http.delete<any, ApiResult<null>>(`/test-scripts/${id}`)
}
