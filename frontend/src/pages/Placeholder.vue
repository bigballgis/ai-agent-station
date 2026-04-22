<template>
  <div class="placeholder-page">
    <a-result
      status="info"
      :title="t('placeholder.comingSoon')"
      :sub-title="t('placeholder.description')"
    >
      <template #extra>
        <a-button type="primary" key="console" @click="goBack">
          {{ t('placeholder.goBack') }}
        </a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()

function goBack() {
  router.back()
}
</script>

<style scoped>
.placeholder-page {
  padding: 20px 0;
}
</style>
