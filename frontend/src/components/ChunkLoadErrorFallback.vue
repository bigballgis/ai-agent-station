<template>
  <div class="chunk-load-error-fallback text-center p-8">
    <div class="chunk-load-icon mb-4">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="mx-auto text-amber-500 dark:text-amber-400">
        <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    </div>
    <h3 class="text-base font-semibold text-neutral-700 dark:text-neutral-200 mb-2">
      {{ t('password.chunkLoadError') }}
    </h3>
    <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-4">
      {{ t('password.chunkLoadErrorHint') }}
    </p>

    <!-- Auto-retry countdown -->
    <div v-if="countdown > 0" class="mb-4">
      <span class="text-xs text-neutral-400 dark:text-neutral-500">
        {{ t('password.autoRetryIn', { seconds: countdown }) }}
      </span>
    </div>

    <!-- Manual actions -->
    <div class="flex items-center justify-center gap-3">
      <button
        @click="handleRetry"
        :disabled="countdown > 0"
        class="btn btn-primary focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {{ t('password.retry') }}
      </button>
      <button
        @click="handleReload"
        class="btn btn-secondary focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none"
      >
        {{ t('password.reloadPage') }}
      </button>
    </div>

    <!-- Error details (collapsible) -->
    <div v-if="errorMessage" class="mt-4 text-left">
      <button
        @click="showDetails = !showDetails"
        class="text-xs text-neutral-400 dark:text-neutral-500 hover:text-neutral-600 dark:hover:text-neutral-300 transition-colors"
      >
        {{ showDetails ? t('password.hideDetails') : t('password.showDetails') }}
      </button>
      <div v-if="showDetails" class="mt-2 p-3 rounded-lg bg-neutral-100 dark:bg-neutral-800 text-xs text-neutral-500 dark:text-neutral-400 font-mono break-all max-h-32 overflow-auto">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  error?: Error
  retry?: () => void
}>()

const emit = defineEmits<{
  retry: []
}>()

const { t } = useI18n()
const countdown = ref(0)
const showDetails = ref(false)
const errorMessage = ref(props.error?.message || '')

let countdownTimer: ReturnType<typeof setInterval> | null = null

function startAutoRetry() {
  countdown.value = 5
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
      handleRetry()
    }
  }, 1000)
}

function handleRetry() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
  if (props.retry) {
    props.retry()
  }
  emit('retry')
}

function handleReload() {
  window.location.reload()
}

onMounted(() => {
  startAutoRetry()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>
