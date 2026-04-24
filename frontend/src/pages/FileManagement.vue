<template>
  <div class="file-management">
    <!-- 页面头部 -->
    <div class="mb-8 animate-fade-in">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
            文件管理
          </h1>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            上传、管理和下载 Agent 相关的文件资源，支持拖拽上传
          </p>
        </div>
        <a-button type="primary" @click="scrollToUpload">
          <template #icon>
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
            </svg>
          </template>
          上传文件
        </a-button>
      </div>
    </div>

    <!-- 上传区域 -->
    <div ref="uploadRef" class="mb-8 animate-slide-up">
      <a-card
        class="rounded-2xl border border-neutral-100 dark:border-neutral-800 shadow-card"
        :body-style="{ padding: '0' }"
      >
        <a-upload-dragger
          :file-list="uploadFileList"
          :before-upload="handleBeforeUpload"
          :multiple="true"
          @remove="handleRemove"
          class="file-upload-dragger"
        >
          <div class="py-10">
            <div class="flex justify-center mb-4">
              <span class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-50 dark:bg-blue-900/30 text-blue-500 dark:text-blue-400">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
              </span>
            </div>
            <p class="text-base font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              点击或拖拽文件到此区域上传
            </p>
            <p class="text-sm text-neutral-400 dark:text-neutral-500">
              支持上传 PDF、Word、Excel、图片等文件，单个文件不超过 50MB
            </p>
          </div>
        </a-upload-dragger>
      </a-card>
    </div>

    <!-- 搜索和筛选 -->
    <div class="mb-6 flex flex-wrap items-center gap-3 animate-slide-up">
      <a-input
        v-model:value="searchQuery"
        :placeholder="t('fileMgmt.searchPlaceholder')"
        allow-clear
        class="flex-1 min-w-[240px] max-w-md"
      >
        <template #prefix>
          <svg class="w-4 h-4 text-neutral-400 dark:text-neutral-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </template>
      </a-input>
      <a-select
        v-model:value="typeFilter"
        :placeholder="t('fileMgmt.fileType')"
        allow-clear
        class="min-w-[140px]"
        @change="handleTypeFilterChange"
      >
        <a-select-option value="">{{ t('fileMgmt.allTypes') }}</a-select-option>
        <a-select-option value="pdf">PDF</a-select-option>
        <a-select-option value="word">Word</a-select-option>
        <a-select-option value="excel">Excel</a-select-option>
        <a-select-option value="image">{{ t('fileMgmt.image') }}</a-select-option>
        <a-select-option value="code">{{ t('fileMgmt.code') }}</a-select-option>
        <a-select-option value="other">{{ t('fileMgmt.other') }}</a-select-option>
      </a-select>
      <a-space>
        <a-button @click="resetFilters">{{ t('common.reset') }}</a-button>
      </a-space>
    </div>

    <!-- 文件列表表格 -->
    <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 overflow-hidden animate-slide-up">
      <a-table
        :columns="columns"
        :data-source="filteredFiles"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (total: number) => t('fileMgmt.totalFiles', { total }) }"
        row-key="id"
        class="file-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'fileName'">
            <div class="flex items-center gap-3">
              <span
                :class="[
                  'inline-flex items-center justify-center w-9 h-9 rounded-lg text-sm font-bold',
                  getFileIconClass(record.type)
                ]"
              >
                {{ getFileIcon(record.type) }}
              </span>
              <div>
                <p class="text-neutral-800 dark:text-neutral-200 font-medium">{{ record.name }}</p>
                <p class="text-xs text-neutral-400 dark:text-neutral-500">{{ record.ext }}</p>
              </div>
            </div>
          </template>

          <template v-if="column.key === 'size'">
            <span class="text-neutral-700 dark:text-neutral-300">{{ formatFileSize(record.size) }}</span>
          </template>

          <template v-if="column.key === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeLabel(record.type) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'uploadTime'">
            <span class="text-neutral-500 dark:text-neutral-400 text-sm">{{ record.uploadTime }}</span>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleDownload(record)">
                下载
              </a-button>
              <a-popconfirm
                :title="t('fileMgmt.deleteConfirm')"
                :ok-text="t('common.confirm')"
                :cancel-text="t('common.cancel')"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { UploadFile } from 'ant-design-vue'
import { uploadFile, downloadFile, getFileList, deleteFile } from '@/api/file'
import { logger } from '@/utils/logger'

interface FileItem {
  id: number
  name: string
  ext: string
  size: number
  type: string
  uploadTime: string
}

const { t } = useI18n()

const loading = ref(false)
const searchQuery = ref('')
const typeFilter = ref<string>('')
const uploadFileList = ref<UploadFile[]>([])
const uploadRef = ref<HTMLElement | null>(null)

const files = ref<FileItem[]>([])

async function fetchFiles() {
  loading.value = true
  try {
    const res = await getFileList()
    files.value = res.data || res || []
  } catch (e) {
    logger.error('获取文件列表失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchFiles()
})

const filteredFiles = computed(() => {
  let result = files.value
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(f => f.name.toLowerCase().includes(query) || f.ext.toLowerCase().includes(query))
  }
  if (typeFilter.value) {
    result = result.filter(f => f.type === typeFilter.value)
  }
  return result
})

const columns = [
  { title: t('fileMgmt.fileName'), key: 'fileName', dataIndex: 'name', width: 280 },
  { title: t('fileMgmt.size'), key: 'size', width: 120 },
  { title: t('common.type'), key: 'type', width: 100, align: 'center' as const },
  { title: t('fileMgmt.uploadTime'), key: 'uploadTime', dataIndex: 'uploadTime', width: 180 },
  { title: t('common.actions'), key: 'action', width: 150, align: 'center' as const },
]

function formatFileSize(bytes: number): string {
  if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}

function getTypeColor(type: string): string {
  const map: Record<string, string> = {
    pdf: 'red',
    word: 'blue',
    excel: 'green',
    image: 'purple',
    code: 'orange',
    other: 'default',
  }
  return map[type] || 'default'
}

function getTypeLabel(type: string): string {
  const map: Record<string, string> = {
    pdf: 'PDF',
    word: 'Word',
    excel: 'Excel',
    image: t('fileMgmt.image'),
    code: t('fileMgmt.code'),
    other: t('fileMgmt.other'),
  }
  return map[type] || type
}

function getFileIcon(type: string): string {
  const map: Record<string, string> = {
    pdf: 'P',
    word: 'W',
    excel: 'E',
    image: 'I',
    code: '</>',
    other: 'F',
  }
  return map[type] || '?'
}

function getFileIconClass(type: string): string {
  const map: Record<string, string> = {
    pdf: 'bg-red-50 dark:bg-red-900/30 text-red-500 dark:text-red-400',
    word: 'bg-blue-50 dark:bg-blue-900/30 text-blue-500 dark:text-blue-400',
    excel: 'bg-green-50 dark:bg-green-900/30 text-green-500 dark:text-green-400',
    image: 'bg-purple-50 dark:bg-purple-900/30 text-purple-500 dark:text-purple-400',
    code: 'bg-orange-50 dark:bg-orange-900/30 text-orange-500 dark:text-orange-400',
    other: 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400',
  }
  return map[type] || map.other
}

async function handleBeforeUpload(file: File) {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error(t('fileMgmt.fileTooLarge'))
    return false
  }
  try {
    await uploadFile(file)
    message.success(`${file.name} ${t('fileMgmt.uploadSuccess')}`)
    await fetchFiles()
  } catch (e) {
    logger.error('文件上传失败:', e)
    message.error(t('fileMgmt.uploadFailed'))
  }
  return false
}

function handleRemove(file: UploadFile) {
  uploadFileList.value = uploadFileList.value.filter(f => f.uid !== file.uid)
}

async function handleDownload(record: FileItem) {
  try {
    const res = await downloadFile(String(record.id))
    const blob = new Blob([res as BlobPart])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${record.name}${record.ext}`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success(`${t('fileMgmt.downloadStart')}: ${record.name}${record.ext}`)
  } catch (e) {
    logger.error('文件下载失败:', e)
    message.error(t('fileMgmt.downloadFailed'))
  }
}

async function handleDelete(record: FileItem) {
  try {
    await deleteFile(record.id)
    message.success(`${t('fileMgmt.deleted')}: ${record.name}`)
    await fetchFiles()
  } catch (e) {
    logger.error('文件删除失败:', e)
    message.error(t('fileMgmt.deleteFailed'))
  }
}

function handleTypeFilterChange() {
  // 筛选器变更由 computed 自动处理
}

function resetFilters() {
  searchQuery.value = ''
  typeFilter.value = ''
}

function scrollToUpload() {
  uploadRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
</script>
