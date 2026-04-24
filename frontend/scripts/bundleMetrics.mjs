/**
 * BundleMetrics - Bundle 大小分析工具
 *
 * 用于在 CI/CD 中验证 bundle 大小是否超过阈值。
 * 可作为独立脚本运行，也可被其他构建脚本导入使用。
 *
 * 使用方式：
 *   node scripts/bundleMetrics.mjs <dist-dir> [max-total-size-kb] [max-chunk-size-kb]
 *
 * 示例：
 *   node scripts/bundleMetrics.mjs dist 500 520
 */

import fs from 'fs'
import path from 'path'
import zlib from 'zlib'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * 计算 gzip 大小
 */
function getGzipSize(buffer) {
  return zlib.gzipSync(buffer).length
}

/**
 * 根据 文件扩展名判断 chunk 类型
 */
function getChunkType(filename) {
  if (filename.endsWith('.js')) return 'js'
  if (filename.endsWith('.css')) return 'css'
  if (/\.(png|jpe?g|gif|svg|ico|woff2?|ttf|eot|webp|avif)$/.test(filename)) return 'asset'
  return 'other'
}

/**
 * 递归扫描目录中的所有文件
 */
function scanDir(dir) {
  const files = []
  const entries = fs.readdirSync(dir, { withFileTypes: true })

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      files.push(...scanDir(fullPath))
    } else {
      files.push(fullPath)
    }
  }

  return files
}

/**
 * 分析 bundle 目录
 * @param {string} distDir - 构建产物目录
 * @param {number} maxTotalSizeKB - 总大小限制（KB），默认 500KB
 * @param {number} maxChunkSizeKB - 单 chunk 大小限制（KB），默认 520KB
 * @returns {object} BundleMetricsResult
 */
export function analyzeBundle(distDir, maxTotalSizeKB = 500, maxChunkSizeKB = 520) {
  const errors = []
  const chunks = []

  if (!fs.existsSync(distDir)) {
    return {
      chunkCount: 0,
      totalSize: 0,
      totalGzipSize: 0,
      chunks: [],
      passed: false,
      errors: [`Directory not found: ${distDir}`],
    }
  }

  const files = scanDir(distDir)
  let totalSize = 0
  let totalGzipSize = 0

  for (const filePath of files) {
    const relativeName = path.relative(distDir, filePath)
    const stat = fs.statSync(filePath)
    const buffer = fs.readFileSync(filePath)

    const chunk = {
      name: relativeName,
      size: stat.size,
      gzipSize: getGzipSize(buffer),
      type: getChunkType(relativeName),
    }

    chunks.push(chunk)
    totalSize += stat.size
    totalGzipSize += chunk.gzipSize

    // 检查单 chunk 大小限制（仅 JS 和 CSS）
    if ((chunk.type === 'js' || chunk.type === 'css') && stat.size > maxChunkSizeKB * 1024) {
      errors.push(
        `Chunk "${relativeName}" exceeds size limit: ${(stat.size / 1024).toFixed(1)}KB > ${maxChunkSizeKB}KB`
      )
    }
  }

  // 检查总大小限制
  const totalSizeKB = totalSize / 1024
  if (totalSizeKB > maxTotalSizeKB) {
    errors.push(
      `Total bundle size exceeds limit: ${totalSizeKB.toFixed(1)}KB > ${maxTotalSizeKB}KB`
    )
  }

  // 按大小降序排序
  chunks.sort((a, b) => b.size - a.size)

  return {
    chunkCount: files.length,
    totalSize,
    totalGzipSize,
    chunks,
    passed: errors.length === 0,
    errors,
  }
}

/**
 * 格式化输出 Bundle 分析报告
 * @param {object} metrics - BundleMetricsResult
 * @returns {string} 格式化报告
 */
export function formatReport(metrics) {
  const lines = []

  lines.push('=== Bundle Metrics Report ===')
  lines.push('')
  lines.push(`Total chunks: ${metrics.chunkCount}`)
  lines.push(`Total size: ${(metrics.totalSize / 1024).toFixed(1)} KB`)
  lines.push(`Total gzip size: ${(metrics.totalGzipSize / 1024).toFixed(1)} KB`)
  lines.push(`Gzip ratio: ${((metrics.totalGzipSize / metrics.totalSize) * 100).toFixed(1)}%`)
  lines.push('')

  // 按类型分组统计
  const typeStats = new Map()
  for (const chunk of metrics.chunks) {
    const existing = typeStats.get(chunk.type) || { count: 0, size: 0, gzipSize: 0 }
    existing.count++
    existing.size += chunk.size
    existing.gzipSize += chunk.gzipSize
    typeStats.set(chunk.type, existing)
  }

  lines.push('--- By Type ---')
  for (const [type, stats] of typeStats) {
    lines.push(
      `  ${type}: ${stats.count} files, ${(stats.size / 1024).toFixed(1)} KB (gzip: ${(stats.gzipSize / 1024).toFixed(1)} KB)`
    )
  }
  lines.push('')

  // Top 10 最大的 chunks
  lines.push('--- Top 10 Largest Chunks ---')
  const topChunks = metrics.chunks.slice(0, 10)
  for (const chunk of topChunks) {
    lines.push(
      `  ${(chunk.size / 1024).toFixed(1).padStart(8)} KB  (gzip: ${(chunk.gzipSize / 1024).toFixed(1).padStart(6)} KB)  ${chunk.name}`
    )
  }
  lines.push('')

  // 错误信息
  if (metrics.errors.length > 0) {
    lines.push('--- Errors ---')
    for (const error of metrics.errors) {
      lines.push(`  FAIL: ${error}`)
    }
    lines.push('')
  }

  lines.push(`Result: ${metrics.passed ? 'PASS' : 'FAIL'}`)

  return lines.join('\n')
}

// CLI 入口
const args = process.argv.slice(2)
if (args.length >= 1) {
  const distDir = args[0]
  const maxTotalSizeKB = parseInt(args[1] || '500', 10)
  const maxChunkSizeKB = parseInt(args[2] || '520', 10)

  const metrics = analyzeBundle(distDir, maxTotalSizeKB, maxChunkSizeKB)
  const report = formatReport(metrics)

  console.log(report)

  if (!metrics.passed) {
    process.exit(1)
  }
}
