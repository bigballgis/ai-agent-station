import request from '@/utils/request'

/**
 * 上传文件
 */
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/v1/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 下载文件
 */
export function downloadFile(filePath: string) {
  return request.get(`/v1/files/download/${filePath}`, {
    responseType: 'blob',
  })
}

/**
 * 获取文件列表
 */
export function getFileList() {
  return request.get('/v1/files/list')
}

/**
 * 删除文件
 */
export function deleteFile(id: number) {
  return request.delete(`/v1/files/${id}`)
}
