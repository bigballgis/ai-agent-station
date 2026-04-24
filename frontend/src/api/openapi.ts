import request from '@/utils/request'

/**
 * 获取 OpenAPI 规范文档
 */
export function getOpenApiSpec() {
  return request.get('/v3/api-docs')
}
