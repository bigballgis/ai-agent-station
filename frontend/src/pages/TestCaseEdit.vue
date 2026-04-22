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
        <a-form-item label="Name" name="name">
          <a-input v-model:value="testCase.name" placeholder="Test case name" />
        </a-form-item>

        <a-form-item label="Description" name="description">
          <a-textarea v-model:value="testCase.description" placeholder="Test case description" />
        </a-form-item>

        <a-form-item label="Test Type" name="testType">
          <a-select v-model:value="testCase.testType" placeholder="Select test type">
            <a-select-option value="api">API Test</a-select-option>
            <a-select-option value="ui">UI Test</a-select-option>
            <a-select-option value="unit">Unit Test</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="Config" name="config">
          <a-textarea
            v-model:value="configStr"
            placeholder="Test configuration (JSON format)"
            rows={8}
          />
        </a-form-item>

        <a-form-item label="Parameters" name="parameters">
          <a-textarea
            v-model:value="parametersStr"
            placeholder="Test parameters (JSON format)"
            rows={4}
          />
        </a-form-item>

        <a-form-item label="Status">
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
import { message } from 'ant-design-vue'
import { testApi } from '@/api/test'
import type { TestCase } from '@/api/test'

const router = useRouter()
const route = useRoute()
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
  name: [{ required: true, message: 'Please input test case name' }],
  testType: [{ required: true, message: 'Please select test type' }],
  config: [{ required: true, message: 'Please input test configuration' }]
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
    message.error('Failed to fetch test case')
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
      message.error('Invalid JSON format in config or parameters')
      return
    }

    loading.value = true
    
    if (isEdit.value) {
      await testApi.updateTestCase(Number(route.params.id), testCase.value)
      message.success('Test case updated successfully')
    } else {
      await testApi.createTestCase(testCase.value)
      message.success('Test case created successfully')
    }

    router.push('/test-cases')
  } catch (error) {
    message.error('Failed to save test case')
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
