<template>
  <div class="form-builder">
    <a-form
      ref="formRef"
      :model="internalModel"
      :layout="layout"
      :label-col="labelCol"
      :wrapper-col="wrapperCol"
      :rules="formRules"
    >
      <a-row :gutter="16">
        <a-col
          v-for="field in fields"
          :key="field.key"
          :span="field.span || 24"
        >
          <a-form-item
            :label="field.label"
            :name="field.key"
            :rules="field.rules"
          >
            <!-- 输入框 -->
            <a-input
              v-if="field.type === 'input'"
              v-model:value="internalModel[field.key]"
              :placeholder="field.placeholder || t('common.pleaseInput', { label: field.label })"
              :disabled="field.disabled"
              :maxlength="field.maxlength"
              :allow-clear="!field.disabled"
            />

            <!-- 文本域 -->
            <a-textarea
              v-else-if="field.type === 'textarea'"
              v-model:value="internalModel[field.key]"
              :placeholder="field.placeholder || t('common.pleaseInput', { label: field.label })"
              :disabled="field.disabled"
              :rows="field.rows || 3"
              :maxlength="field.maxlength"
              :allow-clear="!field.disabled"
              show-count
            />

            <!-- 数字输入 -->
            <a-input-number
              v-else-if="field.type === 'number'"
              v-model:value="internalModel[field.key]"
              :placeholder="field.placeholder || t('common.pleaseInput', { label: field.label })"
              :disabled="field.disabled"
              :min="field.min"
              :max="field.max"
              :step="field.step || 1"
              :precision="field.precision"
              style="width: 100%;"
            />

            <!-- 下拉选择 -->
            <a-select
              v-else-if="field.type === 'select'"
              v-model:value="internalModel[field.key]"
              :placeholder="field.placeholder || t('common.pleaseSelect', { label: field.label })"
              :disabled="field.disabled"
              :options="field.options"
              :mode="field.mode"
              :allow-clear="!field.disabled"
              style="width: 100%;"
            />

            <!-- 单选 -->
            <a-radio-group
              v-else-if="field.type === 'radio'"
              v-model:value="internalModel[field.key]"
              :disabled="field.disabled"
            >
              <a-radio
                v-for="opt in field.options"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </a-radio>
            </a-radio-group>

            <!-- 多选框 -->
            <a-checkbox-group
              v-else-if="field.type === 'checkbox'"
              v-model:value="internalModel[field.key]"
              :disabled="field.disabled"
              :options="field.options"
            />

            <!-- 日期选择 -->
            <a-date-picker
              v-else-if="field.type === 'date'"
              v-model:value="internalModel[field.key]"
              :placeholder="field.placeholder || t('common.pleaseSelect', { label: field.label })"
              :disabled="field.disabled"
              :format="field.format || 'YYYY-MM-DD'"
              value-format="YYYY-MM-DD"
              style="width: 100%;"
            />

            <!-- 日期范围 -->
            <a-range-picker
              v-else-if="field.type === 'dateRange'"
              v-model:value="internalModel[field.key]"
              :placeholder="[t('common.startDate'), t('common.endDate')]"
              :disabled="field.disabled"
              style="width: 100%;"
              value-format="YYYY-MM-DD"
            />

            <!-- 开关 -->
            <a-switch
              v-else-if="field.type === 'switch'"
              v-model:checked="internalModel[field.key]"
              :disabled="field.disabled"
            />

            <!-- 密码输入 -->
            <a-input-password
              v-else-if="field.type === 'password'"
              v-model:value="internalModel[field.key]"
              :placeholder="field.placeholder || t('common.pleaseInput', { label: field.label })"
              :disabled="field.disabled"
              :maxlength="field.maxlength"
            />

            <!-- 自定义插槽 -->
            <slot
              v-else-if="field.type === 'custom'"
              :name="`field-${field.key}`"
              :field="field"
              :model="internalModel"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'

/**
 * FormBuilder 组件
 * 动态表单构建器
 * 根据字段配置自动生成表单项
 * 支持 input、textarea、select、radio、checkbox、date、switch、number 等类型
 * 用于各种创建/编辑弹窗
 */

// 字段定义类型
export interface FormField {
  /** 字段类型 */
  type: 'input' | 'textarea' | 'select' | 'radio' | 'checkbox' | 'date' | 'dateRange' | 'switch' | 'number' | 'password' | 'custom'
  /** 字段标签 */
  label: string
  /** 字段键名 */
  key: string
  /** 占位文本 */
  placeholder?: string
  /** 校验规则 */
  rules?: Rule[]
  /** 下拉选项 */
  options?: Array<{ label: string; value: string | number | boolean }>
  /** 是否禁用 */
  disabled?: boolean
  /** 列宽（24 栅格） */
  span?: number
  /** 最大长度 */
  maxlength?: number
  /** 文本域行数 */
  rows?: number
  /** 数字最小值 */
  min?: number
  /** 数字最大值 */
  max?: number
  /** 数字步长 */
  step?: number
  /** 数字精度 */
  precision?: number
  /** 日期格式 */
  format?: string
  /** 选择模式 */
  mode?: 'multiple' | 'tags'
}

interface Props {
  /** 字段配置 */
  fields: FormField[]
  /** 表单数据（双向绑定） */
  modelValue: Record<string, any>
  /** 表单布局 */
  layout?: 'horizontal' | 'vertical' | 'inline'
  /** 标签列配置 */
  labelCol?: { span: number }
  /** 内容列配置 */
  wrapperCol?: { span: number }
}

const props = withDefaults(defineProps<Props>(), {
  layout: 'vertical',
  labelCol: () => ({ span: 6 }),
  wrapperCol: () => ({ span: 18 }),
})

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any>): void
}>()

const formRef = ref<FormInstance | null>(null)

// 内部表单数据
const internalModel = reactive<Record<string, any>>({ ...props.modelValue })

// 动态生成校验规则
const formRules = computed(() => {
  const rules: Record<string, Rule[]> = {}
  props.fields.forEach(field => {
    if (field.rules) {
      rules[field.key] = field.rules
    }
  })
  return rules
})

// 监听内部数据变化，同步到外部
watch(internalModel, (val) => {
  emit('update:modelValue', { ...val })
}, { deep: true })

// 监听外部数据变化
watch(() => props.modelValue, (val) => {
  Object.assign(internalModel, val)
}, { deep: true })

/**
 * 表单校验
 * @returns 校验是否通过
 */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

/**
 * 重置表单
 */
function resetFields() {
  formRef.value?.resetFields()
}

/**
 * 清空校验信息
 */
function clearValidate() {
  formRef.value?.clearValidate()
}

// 暴露方法
defineExpose({
  validate,
  resetFields,
  clearValidate,
  formRef,
})
</script>

<style scoped>
.form-builder {
  width: 100%;
}

/* 覆盖表单项间距 */
:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #525252;
  font-weight: 500;
}

/* 暗色模式 */
:global(.dark) :deep(.ant-form-item-label > label) {
  color: #a3a3a3;
}
</style>
