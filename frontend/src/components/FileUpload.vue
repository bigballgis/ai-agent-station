<template>
  <div class="file-upload">
    <!-- 拖拽上传区域 -->
    <a-upload-dragger
      v-model:file-list="fileList"
      :action="action"
      :accept="accept"
      :multiple="maxCount > 1"
      :max-count="maxCount"
      :before-upload="handleBeforeUpload"
      :custom-request="handleCustomRequest"
      :disabled="disabled"
      @change="handleChange"
      @remove="handleRemove"
    >
      <div class="upload-dragger-content">
        <div class="upload-icon">
          <InboxOutlined />
        </div>
        <p class="upload-text">
          点击或拖拽文件到此区域上传
        </p>
        <p v-if="accept" class="upload-hint">
          支持格式: {{ accept }}
        </p>
        <p v-if="maxSize" class="upload-hint">
          文件大小不超过 {{ formatFileSize(maxSize) }}
        </p>
      </div>
    </a-upload-dragger>

    <!-- 上传进度列表 -->
    <div v-if="fileList.length > 0" class="upload-file-list">
      <div
        v-for="file in fileList"
        :key="file.uid"
        class="upload-file-item"
      >
        <div class="file-info">
          <FileOutlined class="file-icon" />
          <span class="file-name" :title="file.name">{{ file.name }}</span>
          <span class="file-size">{{ formatFileSize(file.size || 0) }}</span>
        </div>
        <div class="file-actions">
          <!-- 上传进度 -->
          <a-progress
            v-if="file.status === 'uploading'"
            :percent="file.percent || 0"
            :show-info="false"
            size="small"
            class="file-progress"
          />
          <!-- 状态图标 -->
          <CheckCircleOutlined v-else-if="file.status === 'done'" class="status-success" />
          <ExclamationCircleOutlined v-else-if="file.status === 'error'" class="status-error" />
          <!-- 删除按钮 -->
          <DeleteOutlined
            class="delete-btn"
            @click="handleRemoveFile(file)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import {
  InboxOutlined,
  FileOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'

/**
 * FileUpload 组件
 * 文件上传组件，封装 a-upload-dragger
 * 支持拖拽上传、文件类型限制、大小限制、进度显示
 * 用于 FileManagement 等页面
 */

const { t } = useI18n()

interface Props {
  /** 上传接口地址 */
  action?: string
  /** 接受的文件类型 */
  accept?: string
  /** 最大文件大小（字节） */
  maxSize?: number
  /** 最大文件数量 */
  maxCount?: number
  /** 是否禁用 */
  disabled?: boolean
  /** 自定义上传函数 */
  customRequest?: (file: UploadFile) => Promise<void>
}

const props = withDefaults(defineProps<Props>(), {
  maxCount: 5,
})

const emit = defineEmits<{
  (e: 'change', fileList: UploadFile[]): void
  (e: 'success', file: UploadFile): void
  (e: 'error', file: UploadFile, error: Error): void
}>()

const fileList = ref<UploadFile[]>([])

/**
 * 格式化文件大小
 */
function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 上传前校验
 */
const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  // 校验文件大小
  if (props.maxSize && file.size > props.maxSize) {
    message.error(t('fileUpload.fileTooLarge', { size: formatFileSize(props.maxSize) }))
    return false
  }

  // 校验文件数量
  if (fileList.value.length >= props.maxCount) {
    message.error(t('fileUpload.maxFileCount', { count: props.maxCount }))
    return false
  }

  return true
}

/**
 * 自定义上传处理
 */
async function handleCustomRequest(options: any) {
  const file = options.file as UploadFile
  // 更新状态为上传中
  file.status = 'uploading'
  file.percent = 0

  try {
    if (props.customRequest) {
      await props.customRequest(file)
    }
    file.status = 'done'
    file.percent = 100
    emit('success', file)
  } catch (error: any) {
    file.status = 'error'
    emit('error', file, error)
    message.error(`上传失败: ${file.name}`)
  }
}

/**
 * 文件变化处理
 */
function handleChange(info: any) {
  fileList.value = info.fileList
  emit('change', fileList.value)
}

/**
 * 文件删除处理
 */
function handleRemove(file: UploadFile) {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid)
  emit('change', fileList.value)
}

/**
 * 删除文件
 */
function handleRemoveFile(file: UploadFile) {
  handleRemove(file)
}
</script>

<style scoped>
.file-upload {
  width: 100%;
}

/* 拖拽区域样式覆盖 */
:deep(.ant-upload-drag) {
  border-radius: 12px !important;
  border: 2px dashed #d4d4d4 !important;
  background: #fafafa !important;
  transition: all 0.3s;
}

:deep(.ant-upload-drag:hover) {
  border-color: #3b82f6 !important;
}

:deep(.ant-upload-drag .ant-upload) {
  padding: 24px !important;
}

.upload-dragger-content {
  text-align: center;
}

.upload-icon {
  font-size: 40px;
  color: #a3a3a3;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  color: #525252;
  margin: 0 0 8px;
}

.upload-hint {
  font-size: 12px;
  color: #a3a3a3;
  margin: 4px 0 0;
}

/* 文件列表 */
.upload-file-list {
  margin-top: 12px;
}

.upload-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.upload-file-item:hover {
  background: #fafafa;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.file-icon {
  color: #a3a3a3;
  font-size: 16px;
  flex-shrink: 0;
}

.file-name {
  font-size: 13px;
  color: #171717;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: #a3a3a3;
  flex-shrink: 0;
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.file-progress {
  width: 100px;
}

.status-success {
  color: #22c55e;
  font-size: 16px;
}

.status-error {
  color: #ef4444;
  font-size: 16px;
}

.delete-btn {
  color: #a3a3a3;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s;
}

.delete-btn:hover {
  color: #ef4444;
}

/* 暗色模式 */
:global(.dark) :deep(.ant-upload-drag) {
  border-color: #404040 !important;
  background: #1a1a1a !important;
}

:global(.dark) .upload-icon {
  color: #525252;
}

:global(.dark) .upload-text {
  color: #a3a3a3;
}

:global(.dark) .upload-file-item {
  border-color: #262626;
  background: #171717;
}

:global(.dark) .upload-file-item:hover {
  background: #1a1a1a;
}

:global(.dark) .file-name {
  color: #e5e5e5;
}
</style>
