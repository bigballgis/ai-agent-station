<template>
  <div class="evolution-container">
    <a-card :title="t('evolution.title')" class="mb-6">
      <p class="text-gray-500">{{ t('evolution.desc') }}</p>
    </a-card>

    <a-tabs default-active-key="reflection" class="mb-6">
      <a-tab-pane tab="t('evolution.reflection')" key="reflection">
        <Suspense>
          <template #default>
            <ReflectionEvaluation />
          </template>
          <template #fallback>
            <LoadingSkeleton type="chart" :rows="2" />
          </template>
        </Suspense>
      </a-tab-pane>

      <a-tab-pane tab="t('evolution.experience')" key="experience">
        <Suspense>
          <template #default>
            <ExperienceData />
          </template>
          <template #fallback>
            <LoadingSkeleton type="table" :rows="3" />
          </template>
        </Suspense>
      </a-tab-pane>

      <a-tab-pane tab="t('evolution.suggestion')" key="suggestion">
        <Suspense>
          <template #default>
            <OptimizationSuggestion />
          </template>
          <template #fallback>
            <LoadingSkeleton type="table" :rows="3" />
          </template>
        </Suspense>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { defineAsyncComponent } from 'vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
// 三个进化子组件通过 Tab 切换显示，仅激活时才需要加载，使用异步组件按需加载
const ReflectionEvaluation = defineAsyncComponent(() => import('@/components/evolution/ReflectionEvaluation.vue'))
const ExperienceData = defineAsyncComponent(() => import('@/components/evolution/ExperienceData.vue'))
const OptimizationSuggestion = defineAsyncComponent(() => import('@/components/evolution/OptimizationSuggestion.vue'))

const { t } = useI18n()
</script>

<style scoped>
.evolution-container {
  min-height: 800px;
}
</style>