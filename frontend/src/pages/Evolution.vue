<template>
  <div class="evolution-container">
    <a-card :title="t('evolution.title')" class="mb-6">
      <p class="text-gray-500">{{ t('evolution.desc') }}</p>
    </a-card>

    <a-tabs default-active-key="reflection" class="mb-6">
      <a-tab-pane tab="t('evolution.reflection')" key="reflection">
        <ReflectionEvaluation />
      </a-tab-pane>

      <a-tab-pane tab="t('evolution.experience')" key="experience">
        <ExperienceData />
      </a-tab-pane>

      <a-tab-pane tab="t('evolution.suggestion')" key="suggestion">
        <OptimizationSuggestion />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ReflectionEvaluation from '@/components/evolution/ReflectionEvaluation.vue'
import ExperienceData from '@/components/evolution/ExperienceData.vue'
import OptimizationSuggestion from '@/components/evolution/OptimizationSuggestion.vue'

const { t } = useI18n()
</script>

<style scoped>
.evolution-container {
  min-height: 800px;
}
</style>