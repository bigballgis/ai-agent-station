<template>
  <slot v-if="!error" />
  <div v-else class="error-boundary p-6 text-center">
    <h3 class="text-lg font-semibold text-red-500 dark:text-red-400 mb-2">{{ t('common.renderError') }}</h3>
    <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-4">{{ error.message }}</p>
    <button @click="reset" class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
      {{ t('common.retry') }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const error = ref<Error | null>(null)

onErrorCaptured((err) => {
  error.value = err
  return false
})

function reset() {
  error.value = null
}
</script>
