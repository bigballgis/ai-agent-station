import { reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'

// ==================== 类型定义 ====================

export interface ValidationRule {
  /** 规则类型 */
  type?: 'required' | 'email' | 'minLength' | 'maxLength' | 'pattern' | 'custom'
  /** 必填验证 */
  required?: boolean
  /** 邮箱验证 */
  email?: boolean
  /** 最小长度 */
  min?: number
  /** 最大长度 */
  max?: number
  /** 正则表达式 */
  pattern?: RegExp
  /** 自定义验证函数 */
  validator?: (value: unknown, formData?: Record<string, unknown>) => string | null | Promise<string | null>
  /** 自定义错误消息（覆盖默认消息） */
  message?: string
  /** 触发时机 */
  trigger?: 'blur' | 'change' | 'submit'
}

export interface FieldValidation {
  rules: ValidationRule[]
  error: string
  touched: boolean
}

export type FormErrors = Record<string, string>
export type FormRules = Record<string, ValidationRule[]>

// ==================== 内置验证器 ====================

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 内置验证规则执行器
 */
async function executeRule(rule: ValidationRule, value: unknown, formData?: Record<string, unknown>): Promise<string | null> {
  // Required
  if (rule.required) {
    if (value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
      return rule.message || null // message will be set by composable using i18n
    }
  }

  // Skip other validations if value is empty and not required
  if (value === null || value === undefined || value === '') {
    return null
  }

  const strValue = String(value)

  // Email
  if (rule.email || rule.type === 'email') {
    if (!EMAIL_REGEX.test(strValue)) {
      return rule.message || null
    }
  }

  // MinLength
  if ((rule.min !== undefined && rule.type === 'minLength') || (rule.min !== undefined && rule.required)) {
    if (strValue.length < rule.min) {
      return rule.message || null
    }
  }

  // MaxLength
  if ((rule.max !== undefined && rule.type === 'maxLength') || (rule.max !== undefined && rule.required)) {
    if (strValue.length > rule.max) {
      return rule.message || null
    }
  }

  // Pattern
  if (rule.pattern) {
    if (!rule.pattern.test(strValue)) {
      return rule.message || null
    }
  }

  // Custom validator
  if (rule.validator) {
    return rule.validator(value, formData)
  }

  return null
}

// ==================== useFormValidation Composable ====================

/**
 * useFormValidation composable
 *
 * 提供统一的表单验证能力：
 * - validateField(field, value, rules) - 验证单个字段
 * - validateForm(fields, values) - 验证整个表单
 * - formErrors ref - 响应式错误映射
 * - isFormValid computed - 整体有效性
 * - 内置规则：required, email, minLength, maxLength, pattern, custom
 */
export function useFormValidation<T extends Record<string, unknown> = Record<string, unknown>>(
  initialRules: FormRules = {}
) {
  const { t } = useI18n()

  const formErrors = reactive<FormErrors>({})
  const touchedFields = reactive<Record<string, boolean>>({})
  const rules = reactive<FormRules>({ ...initialRules })

  /**
   * 获取规则对应的 i18n 错误消息
   */
  function getRuleErrorMessage(rule: ValidationRule, _field: string): string {
    if (rule.message) return rule.message

    if (rule.required) {
      return t('common.validation.required')
    }
    if (rule.email || rule.type === 'email') {
      return t('common.validation.email')
    }
    if (rule.type === 'minLength' && rule.min !== undefined) {
      return t('common.validation.minLength', { min: rule.min })
    }
    if (rule.type === 'maxLength' && rule.max !== undefined) {
      return t('common.validation.maxLength', { max: rule.max })
    }
    if (rule.pattern) {
      return t('common.validation.pattern')
    }
    if (rule.type === 'custom') {
      return t('common.validation.custom')
    }

    return t('common.validation.custom')
  }

  /**
   * 验证单个字段
   */
  async function validateField(field: string, value: unknown, fieldRules?: ValidationRule[], formData?: Record<string, unknown>): Promise<boolean> {
    const activeRules = fieldRules || rules[field]
    if (!activeRules || activeRules.length === 0) {
      // No rules means no error
      delete formErrors[field]
      return true
    }

    for (const rule of activeRules) {
      const error = await executeRule(rule, value, formData)
      if (error !== null) {
        formErrors[field] = error || getRuleErrorMessage(rule, field)
        return false
      }
    }

    delete formErrors[field]
    return true
  }

  /**
   * 验证整个表单
   */
  async function validateForm(values: T): Promise<boolean> {
    let isValid = true

    for (const field of Object.keys(rules)) {
      const fieldRules = rules[field]
      if (fieldRules && fieldRules.length > 0) {
        const valid = await validateField(field, values[field], fieldRules, values as Record<string, unknown>)
        if (!valid) {
          isValid = false
        }
      }
    }

    return isValid
  }

  /**
   * 标记字段为已触碰（用于 blur 触发验证）
   */
  function touchField(field: string) {
    touchedFields[field] = true
  }

  /**
   * 重置所有错误和触碰状态
   */
  function resetValidation() {
    for (const key of Object.keys(formErrors)) {
      delete formErrors[key]
    }
    for (const key of Object.keys(touchedFields)) {
      delete touchedFields[key]
    }
  }

  /**
   * 获取字段错误
   */
  function getFieldError(field: string): string {
    return formErrors[field] || ''
  }

  /**
   * 设置字段错误（手动设置）
   */
  function setFieldError(field: string, error: string) {
    formErrors[field] = error
  }

  /**
   * 清除字段错误
   */
  function clearFieldError(field: string) {
    delete formErrors[field]
  }

  /**
   * 更新验证规则
   */
  function setRules(newRules: FormRules) {
    // Clear existing rules
    for (const key of Object.keys(rules)) {
      delete rules[key]
    }
    // Apply new rules
    Object.assign(rules, newRules)
  }

  /**
   * 整体表单是否有效
   */
  const isFormValid = computed(() => {
    return Object.keys(formErrors).length === 0
  })

  /**
   * 获取第一个错误字段
   */
  const firstErrorField = computed(() => {
    const keys = Object.keys(formErrors)
    return keys.length > 0 ? keys[0] : null
  })

  return {
    formErrors,
    touchedFields,
    rules,
    isFormValid,
    firstErrorField,
    validateField,
    validateForm,
    touchField,
    resetValidation,
    getFieldError,
    setFieldError,
    clearFieldError,
    setRules,
  }
}
