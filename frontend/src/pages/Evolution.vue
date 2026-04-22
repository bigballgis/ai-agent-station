<template>
  <div class="evolution-container">
    <a-card title="Agent自进化系统" class="mb-6">
      <p class="text-gray-500">通过持续的反思、学习和优化，提升Agent的性能和智能水平</p>
    </a-card>

    <a-tabs default-active-key="reflection" class="mb-6">
      <a-tab-pane tab="反思评估" key="reflection">
        <ReflectionEvaluation />
      </a-tab-pane>

      <a-tab-pane tab="经验数据" key="experience">
        <ExperienceData />
      </a-tab-pane>

      <a-tab-pane tab="优化建议" key="suggestion">
        <OptimizationSuggestion />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import ReflectionEvaluation from '@/components/evolution/ReflectionEvaluation.vue'
import ExperienceData from '@/components/evolution/ExperienceData.vue'
import OptimizationSuggestion from '@/components/evolution/OptimizationSuggestion.vue'
</script>

<style scoped>
.evolution-container {
  min-height: 800px;
}
</style>