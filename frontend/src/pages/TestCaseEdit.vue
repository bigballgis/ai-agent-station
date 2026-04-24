<template>
  <div class="test-case-edit">
    <div class="page-header">
      <h1>{{ isEdit ? $t('test.editTestCase') : $t('test.createTestCase') }}</h1>
      <a-button @click="handleCancel">
        {{ $t('common.cancel') }}
      </a-button>
    </div>

    <a-card>
      <a-form
        :model="testCase"
        :rules="rules"
        ref="formRef"
        layout="vertical"
      >
        <a-form-item :label="$t('testCaseEdit.nameLabel')" name="name">
          <a-input v-model:value="testCase.name" :placeholder="$t('testCaseEdit.namePlaceholder')" />
        </a-form-item>

        <a-form-item :label="$t('testCaseEdit.descriptionLabel')" name="description">
          <a-textarea v-model:value="testCase.description" :placeholder="$t('testCaseEdit.descriptionPlaceholder')" />
        </a-form-item>

        <a-form-item :label="$t('testCaseEdit.testTypeLabel')" name="testType">
          <a-select v-model:value="testCase.testType" :placeholder="$t('testCaseEdit.selectTestType')">
            <a-select-option value="api">{{ $t('testCaseEdit.apiTest') }}</a-select-option>
            <a-select-option value="ui">{{ $t('testCaseEdit.uiTest') }}</a-select-option>
            <a-select-option value="unit">{{ $t('testCaseEdit.unitTest') }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('testCaseEdit.configLabel')" name="config">
          <a-textarea
            v-model:value="configStr"
            :placeholder="$t('testCaseEdit.configPlaceholder')"
            rows={8}
          />
        </a-form-item>

        <a-form-item :label="$t('testCaseEdit.parametersLabel')" name="parameters">
          <a-textarea
            v-model:value="parametersStr"
            :placeholder="$t('testCaseEdit.parametersPlaceholder')"
            rows={4}
          />
        </a-form-item>

        <a-form-item :label="$t('testCaseEdit.statusLabel')">
          <a-switch v-model:checked="testCase.isActive" />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" @click="handleSubmit" :loading="loading">
            {{ $t('common.save') }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { testApi } from '@/api/test'
import type { TestCase } from '@/api/test'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const formRef = ref()
const loading = ref(false)
const testCase = ref<TestCase>({
  name: '',
  description: '',
  testType: 'api',
  config: {},
  parameters: {},
  isActive: true
})

const configStr = ref('{\n  "url": "",\n  "method": "GET",\n  "headers": {},\n  "body": {}\n}')
const parametersStr = ref('{\n  "param1": "value1",\n  "param2": "value2"\n}')

const rules = {
  name: [{ required: true, message: () => t('testCaseEdit.nameRequired') }],
  testType: [{ required: true, message: () => t('testCaseEdit.testTypeRequired') }],
  config: [{ required: true, message: () => t('testCaseEdit.configRequired') }]
}

const isEdit = computed(() => !!route.params.id)

const fetchTestCase = async () => {
  if (!isEdit.value) return

  const id = Number(route.params.id)
  loading.value = true
  try {
    const response = await testApi.getTestCaseById(id)
    testCase.value = response.data
    configStr.value = JSON.stringify(testCase.value.config, null, 2)
    parametersStr.value = JSON.stringify(testCase.value.parameters || {}, null, 2)
  } catch (error) {
    message.error(t('testCaseEdit.fetchFailed'))
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    // Parse JSON strings
    try {
      testCase.value.config = JSON.parse(configStr.value)
      testCase.value.parameters = JSON.parse(parametersStr.value)
    } catch (e) {
      message.error(t('testCaseEdit.jsonParseError'))
      return
    }

    loading.value = true

    if (isEdit.value) {
      await testApi.updateTestCase(Number(route.params.id), testCase.value)
      message.success(t('testCaseEdit.updateSuccess'))
    } else {
      await testApi.createTestCase(testCase.value)
      message.success(t('testCaseEdit.createSuccess'))
    }

    router.push('/test-cases')
  } catch (error) {
    message.error(t('testCaseEdit.saveFailed'))
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.push('/test-cases')
}

onMounted(() => {
  fetchTestCase()
})
</script>

<style scoped>
.test-case-edit {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
