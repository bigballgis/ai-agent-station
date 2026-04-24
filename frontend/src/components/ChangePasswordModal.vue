<template>
  <a-modal
    :open="visible"
    :title="t('password.changePassword')"
    :confirm-loading="loading"
    :ok-text="t('common.confirm')"
    :cancel-text="t('common.cancel')"
    @ok="handleSubmit"
    @cancel="handleClose"
    :width="440"
    destroyOnClose
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
      class="mt-4"
    >
      <a-form-item :label="t('password.oldPassword')" name="oldPassword">
        <a-input-password
          v-model:value="formState.oldPassword"
          :placeholder="t('password.oldPasswordPlaceholder')"
          size="large"
        >
          <template #prefix>
            <LockOutlined class="text-neutral-400" />
          </template>
        </a-input-password>
      </a-form-item>

      <a-form-item :label="t('password.newPassword')" name="newPassword">
        <a-input-password
          v-model:value="formState.newPassword"
          :placeholder="t('password.newPasswordPlaceholder')"
          size="large"
        >
          <template #prefix>
            <LockOutlined class="text-neutral-400" />
          </template>
        </a-input-password>
      </a-form-item>

      <a-form-item :label="t('password.confirmPassword')" name="confirmPassword">
        <a-input-password
          v-model:value="formState.confirmPassword"
          :placeholder="t('password.confirmPasswordPlaceholder')"
          size="large"
        >
          <template #prefix>
            <LockOutlined class="text-neutral-400" />
          </template>
        </a-input-password>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { LockOutlined } from '@ant-design/icons-vue'
import { changePassword } from '@/api/user'

const { t } = useI18n()

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const formRef = ref()
const loading = ref(false)

const formState = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = async (_rule: unknown, value: string) => {
  if (value && value !== formState.newPassword) {
    return Promise.reject(t('password.passwordMismatch'))
  }
  return Promise.resolve()
}

const rules = {
  oldPassword: [
    { required: true, message: () => t('password.oldPasswordRequired'), trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: () => t('password.newPasswordRequired'), trigger: 'blur' },
    { min: 6, message: () => t('password.newPasswordMinLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: () => t('password.confirmPasswordRequired'), trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
    loading.value = true

    await changePassword({
      oldPassword: formState.oldPassword,
      newPassword: formState.newPassword,
    })

    message.success(t('password.changePasswordSuccess'))
    handleClose()
    emit('success')
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    message.error(err?.response?.data?.message || t('password.changePasswordFailed'))
  } finally {
    loading.value = false
  }
}

function handleClose() {
  formState.oldPassword = ''
  formState.newPassword = ''
  formState.confirmPassword = ''
  emit('update:visible', false)
}
</script>
