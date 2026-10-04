import http from './http'
import type { ApiResult } from './types'

// ========== 类型定义 ==========

export interface Project {
  id: number
  name: string
  description: string
  testCaseCount: number
  testScriptCount: number
  testDataCount: number
  createdAt: string
  updatedAt: string
}

export interface ProjectCreateDTO {
  name: string
  description: string
}

export interface ProjectUpdateDTO {
  name?: string
  description?: string
}

export interface ProjectPageParams {
  page?: number
  pageSize?: number
  keyword?: string
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
 * 分页获取项目列表
 */
export const getProjects = (params?: ProjectPageParams) => {
  return http.get<any, ApiResult<PageResult<Project>>>('/projects', { params })
}

/**
 * 获取项目详情
 */
export const getProject = (id: number) => {
  return http.get<any, ApiResult<Project>>(`/projects/${id}`)
}

/**
 * 创建项目
 */
export const createProject = (data: ProjectCreateDTO) => {
  return http.post<any, ApiResult<Project>>('/projects', data)
}

/**
 * 更新项目
 */
export const updateProject = (id: number, data: ProjectUpdateDTO) => {
  return http.put<any, ApiResult<Project>>(`/projects/${id}`, data)
}

/**
 * 删除项目
 */
export const deleteProject = (id: number) => {
  return http.delete<any, ApiResult<null>>(`/projects/${id}`)
}
