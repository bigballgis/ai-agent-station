<template>
  <div class="not-found-container flex flex-col items-center justify-center min-h-[60vh]" role="alert">
    <div class="text-center">
      <h1 class="text-9xl font-bold text-gray-200 dark:text-neutral-700 mb-4" aria-hidden="true">404</h1>
      <h2 class="text-2xl font-semibold text-gray-600 dark:text-neutral-300 mb-4">{{ t('notFound.title') }}</h2>
      <p class="text-gray-500 dark:text-neutral-400 mb-8">{{ t('notFound.description') }}</p>
      <a-button type="primary" size="large" @click="goHome">
        {{ t('notFound.backHome') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.not-found-container {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

h1 {
  font-size: 8rem;
  font-weight: bold;
  color: #e5e7eb;
  margin-bottom: 1rem;
}

h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #4b5563;
  margin-bottom: 1rem;
}

p {
  color: #6b7280;
  margin-bottom: 2rem;
}

:global(.dark) h1 {
  color: #404040;
}

:global(.dark) h2 {
  color: #d4d4d4;
}

:global(.dark) p {
  color: #a3a3a3;
}
</style>
