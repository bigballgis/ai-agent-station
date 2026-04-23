/**
 * 统一 API 响应类型
 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 分页结果（对齐后端 Spring Data 分页结构）
 */
export interface PageResult<T = unknown> {
  records: T[]
  total: number
  totalPages: number
  page: number
  size: number
}

/**
 * 分页查询参数
 */
export interface PageRequest {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export interface SelectOption {
  label: string
  value: string | number
  disabled?: boolean
  children?: SelectOption[]
}

export interface TreeNode {
  key: string
  title: string
  children?: TreeNode[]
  isLeaf?: boolean
  icon?: string
}
