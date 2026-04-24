import request from '@/utils/request'

export interface TestCase {
  id?: number
  tenantId?: number
  agentId?: number
  name: string
  description?: string
  testType: string
  config: Record<string, any>
  parameters?: Record<string, any>
  isActive?: boolean
  createdBy?: number
  updatedBy?: number
  createdAt?: string
  updatedAt?: string
}

export interface TestExecution {
  id?: number
  tenantId?: number
  testCaseId: number
  testCaseName?: string
  status: 'pending' | 'running' | 'completed' | 'failed' | 'canceled'
  startTime?: string
  endTime?: string
  result?: Record<string, any>
  errorMessage?: string
  createdBy?: number
  createdAt?: string
}

export interface TestResult {
  id?: number
  tenantId?: number
  testExecutionId: number
  testCaseId: number
  testCaseName?: string
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED'
  executionTime: number
  errorMessage?: string
  details?: Record<string, any>
  createdAt?: string
}

export interface TestCaseVersion {
  id?: number
  testCaseId: number
  tenantId?: number
  versionNumber: number
  config: Record<string, any>
  changeLog?: string
  createdBy?: number
  createdAt?: string
}

export const testApi = {
  // 测试用例相关
  getAllTestCases: () => {
    return request.get<TestCase[]>('/v1/test-cases')
  },

  getTestCaseById: (id: number) => {
    return request.get<TestCase>(`/v1/test-cases/${id}`)
  },

  createTestCase: (data: Partial<TestCase>) => {
    return request.post<TestCase>('/v1/test-cases', data)
  },

  updateTestCase: (id: number, data: Partial<TestCase>) => {
    return request.put<TestCase>(`/v1/test-cases/${id}`, data)
  },

  deleteTestCase: (id: number) => {
    return request.delete(`/v1/test-cases/${id}`)
  },

  // NOTE: Backend endpoint not yet implemented
  copyTestCase: (id: number, newName: string) => {
    return request.post<TestCase>(`/v1/test-cases/${id}/copy`, { newName })
  },

  // 测试用例版本管理
  // NOTE: Backend endpoint not yet implemented
  getTestCaseVersions: (id: number) => {
    return request.get<TestCaseVersion[]>(`/v1/test-cases/${id}/versions`)
  },

  // NOTE: Backend endpoint not yet implemented
  getTestCaseVersion: (id: number, versionNumber: number) => {
    return request.get<TestCaseVersion>(`/v1/test-cases/${id}/versions/${versionNumber}`)
  },

  // NOTE: Backend endpoint not yet implemented
  rollbackToVersion: (id: number, versionNumber: number) => {
    return request.post<TestCase>(`/v1/test-cases/${id}/versions/${versionNumber}/rollback`)
  },

  // 测试执行相关
  createTestExecution: (data: Partial<TestExecution>) => {
    return request.post<TestExecution>('/v1/test-executions', data)
  },

  // NOTE: Backend endpoint not yet implemented
  createBatchTestExecutions: (testCaseIds: number[]) => {
    return request.post<TestExecution[]>('/v1/test-executions/batch', { testCaseIds })
  },

  getTestExecutionById: (id: number) => {
    return request.get<TestExecution>(`/v1/test-executions/${id}`)
  },

  getTestExecutions: (params?: Record<string, any>) => {
    return request.get<TestExecution[]>('/v1/test-executions', { params })
  },

  cancelTestExecution: (id: number) => {
    return request.post(`/v1/test-executions/${id}/cancel`)
  },

  // 测试结果相关
  getTestResults: (params?: Record<string, any>) => {
    return request.get<TestResult[]>('/v1/test-results', { params })
  },

  getTestResultById: (id: number) => {
    return request.get<TestResult>(`/v1/test-results/${id}`)
  },

  getTestResultsByExecutionId: (executionId: number) => {
    return request.get<TestResult[]>(`/v1/test-results/execution/${executionId}`)
  },

  exportTestResults: (params?: Record<string, any>) => {
    return request.get('/v1/export/test-results', { params, responseType: 'blob' })
  }
}
