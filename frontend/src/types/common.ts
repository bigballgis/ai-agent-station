export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

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
