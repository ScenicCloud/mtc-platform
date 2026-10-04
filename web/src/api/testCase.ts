import http from './http'
import type { ApiResult } from './types'

// ========== 类型定义 ==========

export interface TestCase {
  id: number
  projectId: number
  title: string
  module: string
  priority: 'P0' | 'P1' | 'P2' | 'P3'
  status: 'draft' | 'active' | 'deprecated'
  precondition: string
  steps: TestStep[]
  expectedResult: string
  createdAt: string
  updatedAt: string
}

export interface TestStep {
  id?: number
  order: number
  action: string
  expected: string
}

export interface TestCaseCreateDTO {
  projectId: number
  title: string
  module?: string
  priority?: TestCase['priority']
  precondition?: string
  steps?: TestStep[]
  expectedResult?: string
}

export interface TestCaseUpdateDTO {
  title?: string
  module?: string
  priority?: TestCase['priority']
  status?: TestCase['status']
  precondition?: string
  steps?: TestStep[]
  expectedResult?: string
}

export interface TestCasePageParams {
  page?: number
  pageSize?: number
  keyword?: string
  module?: string
  priority?: string
  status?: string
}

export interface TestCaseBatchCreateDTO {
  projectId: number
  testCases: Omit<TestCaseCreateDTO, 'projectId'>[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// ========== API 方法 ==========

/**
 * 获取项目下的测试用例列表（分页）
 */
export const getTestCases = (projectId: number, params?: TestCasePageParams) => {
  return http.get<any, ApiResult<PageResult<TestCase>>>(
    `/projects/${projectId}/test-cases`,
    { params }
  )
}

/**
 * 获取测试用例详情
 */
export const getTestCase = (id: number) => {
  return http.get<any, ApiResult<TestCase>>(`/test-cases/${id}`)
}

/**
 * 创建测试用例
 */
export const createTestCase = (data: TestCaseCreateDTO) => {
  return http.post<any, ApiResult<TestCase>>('/test-cases', data)
}

/**
 * 批量创建测试用例
 */
export const batchCreateTestCases = (data: TestCaseBatchCreateDTO) => {
  return http.post<any, ApiResult<TestCase[]>>('/test-cases/batch', data)
}

/**
 * 更新测试用例
 */
export const updateTestCase = (id: number, data: TestCaseUpdateDTO) => {
  return http.put<any, ApiResult<TestCase>>(`/test-cases/${id}`, data)
}

/**
 * 删除测试用例
 */
export const deleteTestCase = (id: number) => {
  return http.delete<any, ApiResult<null>>(`/test-cases/${id}`)
}
