import i18n from '@/locales'

/**
 * 后端 messageCode -> 前端 i18n key 的映射表
 *
 * 后端返回格式示例: { code: 400, message: "Agent not found", messageCode: "error.agent.not_found" }
 * 前端通过 messageCode 查找对应的 i18n 翻译，找不到时回退到后端 message 或默认兜底文案
 */
const MESSAGE_CODE_MAP: Record<string, string> = {
  // Agent 相关
  'error.agent.not_found': 'error.agentNotFound',
  'error.agent.already_published': 'error.agentAlreadyPublished',
  'error.agent.create_failed': 'error.agentCreateFailed',
  'error.agent.update_failed': 'error.agentUpdateFailed',
  'error.agent.delete_failed': 'error.agentDeleteFailed',
  'error.agent.copy_failed': 'error.agentCopyFailed',
  'error.agent.name_exists': 'error.agentNameExists',
  'error.agent.status_invalid': 'error.agentStatusInvalid',

  // 工作流相关
  'error.workflow.not_found': 'error.workflowNotFound',
  'error.workflow.start_failed': 'error.workflowStartFailed',
  'error.workflow.cancel_failed': 'error.workflowCancelFailed',

  // 认证相关
  'error.auth.bad_credentials': 'error.auth.badCredentials',
  'error.auth.token_expired': 'error.auth.tokenExpired',
  'error.auth.token_invalid': 'error.auth.tokenInvalid',
  'error.auth.user_exists': 'error.auth.userExists',
  'error.auth.user_not_found': 'error.auth.userNotFound',
  'error.auth.account_locked': 'error.auth.accountLocked',
  'error.auth.password_mismatch': 'error.auth.passwordMismatch',

  // 权限相关
  'error.permission.denied': 'error.permission.denied',
  'error.permission.insufficient': 'error.permission.insufficient',

  // 资源相关
  'error.resource.not_found': 'error.resource.notFound',
  'error.resource.already_exists': 'error.resource.alreadyExists',
  'error.resource.conflict': 'error.resource.conflict',

  // 参数校验
  'error.validation.failed': 'error.validation.failed',
  'error.validation.required': 'error.validation.required',
  'error.validation.format': 'error.validation.format',

  // 限流
  'error.rate_limit.exceeded': 'error.rateLimit.exceeded',

  // 通用
  'error.common.operation_failed': 'error.common.operationFailed',
  'error.common.internal_error': 'error.common.internalError',
  'error.common.service_unavailable': 'error.common.serviceUnavailable',
}

/**
 * 根据 messageCode 获取对应的 i18n 翻译文本
 *
 * @param messageCode 后端返回的错误码，如 "error.agent.not_found"
 * @param fallbackMessage 当找不到映射或 i18n key 时使用的兜底文案
 * @returns 翻译后的错误消息
 */
export function getErrorMessage(messageCode: string, fallbackMessage: string): string {
  const i18nKey = MESSAGE_CODE_MAP[messageCode]
  if (i18nKey) {
    const translated = i18n.global.t(i18nKey)
    // vue-i18n 在找不到 key 时会返回 key 本身（fallbackLocale 模式下）
    // 如果翻译结果不等于 key 本身，说明找到了有效翻译
    if (translated !== i18nKey) {
      return translated
    }
  }
  return fallbackMessage
}

/**
 * 从 axios error response 中提取 messageCode 并获取翻译后的错误消息
 *
 * @param errorResponse axios error.response.data 对象
 * @param fallbackMessage 兜底文案
 * @returns 翻译后的错误消息
 */
export function getErrorDisplayMessage(
  errorResponse: Record<string, unknown> | undefined,
  fallbackMessage: string
): string {
  if (!errorResponse) return fallbackMessage

  const messageCode = errorResponse.messageCode as string | undefined
  const message = errorResponse.message as string | undefined

  if (messageCode) {
    return getErrorMessage(messageCode, message || fallbackMessage)
  }

  return message || fallbackMessage
}

export default MESSAGE_CODE_MAP
