<template>
  <div class="markdown-renderer">
    <!-- 目录（可选） -->
    <div v-if="showToc && tocItems.length > 0" class="markdown-toc">
      <div class="toc-title">目录</div>
      <nav class="toc-list">
        <a
          v-for="item in tocItems"
          :key="item.id"
          :href="`#${item.id}`"
          class="toc-item"
          :class="`toc-level-${item.level}`"
          @click.prevent="scrollToHeading(item.id)"
        >
          {{ item.text }}
        </a>
      </nav>
    </div>

    <!-- Markdown 内容 -->
    <div
      ref="contentRef"
      class="markdown-content"
      v-html="renderedContent"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

/**
 * MarkdownRenderer 组件
 * Markdown 渲染组件
 * 支持基础 Markdown 语法渲染、代码高亮、目录生成
 * 用于 ApiDocumentation 等页面
 */

interface TocItem {
  id: string
  text: string
  level: number
}

interface Props {
  /** Markdown 内容 */
  content: string
  /** 是否显示目录 */
  showToc?: boolean
  /** 最大目录层级 */
  maxTocLevel?: number
}

const props = withDefaults(defineProps<Props>(), {
  showToc: false,
  maxTocLevel: 3,
})

const contentRef = ref<HTMLElement | null>(null)

/**
 * URL 协议白名单校验，防止 XSS 攻击
 */
function sanitizeUrl(url: string): string {
  if (!url) return ''
  const trimmed = url.trim().toLowerCase()
  // 仅允许 http, https, mailto 协议
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('mailto:')) {
    return url.trim()
  }
  // 相对路径允许
  if (trimmed.startsWith('/') || trimmed.startsWith('#') || trimmed.startsWith('./') || trimmed.startsWith('../')) {
    return url.trim()
  }
  return '#'
}

/**
 * 简易 Markdown 渲染
 * 注意：生产环境建议使用 marked + highlight.js
 * 此处提供基础渲染能力
 */
const renderedContent = computed(() => {
  if (!props.content) return ''

  let html = props.content

  // 转义 HTML
  html = html
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 代码块（```language ... ```）
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_match, lang, code) => {
    const language = lang || 'text'
    return `<pre class="md-code-block"><code class="language-${language}">${code.trim()}</code></pre>`
  })

  // 行内代码（`code`）
  html = html.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>')

  // 标题（h1-h6）
  html = html.replace(/^######\s+(.+)$/gm, (_m, text) => `<h6 id="${generateId(text)}">${text}</h6>`)
  html = html.replace(/^#####\s+(.+)$/gm, (_m, text) => `<h5 id="${generateId(text)}">${text}</h5>`)
  html = html.replace(/^####\s+(.+)$/gm, (_m, text) => `<h4 id="${generateId(text)}">${text}</h4>`)
  html = html.replace(/^###\s+(.+)$/gm, (_m, text) => `<h3 id="${generateId(text)}">${text}</h3>`)
  html = html.replace(/^##\s+(.+)$/gm, (_m, text) => `<h2 id="${generateId(text)}">${text}</h2>`)
  html = html.replace(/^#\s+(.+)$/gm, (_m, text) => `<h1 id="${generateId(text)}">${text}</h1>`)

  // 粗体（**text**）
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // 斜体（*text*）
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // 链接（[text](url)）
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_m, text, url) => `<a href="${sanitizeUrl(url)}" target="_blank" rel="noopener">${text}</a>`)

  // 图片（![alt](url)）
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_m, alt, url) => `<img src="${sanitizeUrl(url)}" alt="${alt}" />`)

  // 无序列表
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')

  // 有序列表
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li>$1</li>')

  // 引用（> text）
  html = html.replace(/^&gt;\s+(.+)$/gm, '<blockquote>$1</blockquote>')
  html = html.replace(/<\/blockquote>\n<blockquote>/g, '\n')

  // 分割线（---）
  html = html.replace(/^---$/gm, '<hr />')

  // 表格（简易支持）
  html = html.replace(/^\|(.+)\|$/gm, (_match, content) => {
    const cells = content.split('|').map((c: string) => c.trim())
    // 跳过分隔行
    if (cells.every((c: string) => /^[-:]+$/.test(c))) return ''
    const tag = 'td'
    const row = cells.map((c: string) => `<${tag}>${c}</${tag}>`).join('')
    return `<tr>${row}</tr>`
  })
  html = html.replace(/(<tr>.*<\/tr>\s*)+/g, '<table>$&</table>')

  // 段落（双换行）
  html = html.replace(/\n\n/g, '</p><p>')
  html = `<p>${html}</p>`

  // 清理空段落
  html = html.replace(/<p>\s*<\/p>/g, '')
  html = html.replace(/<p>\s*(<(h[1-6]|pre|ul|ol|blockquote|table|hr))/g, '$1')
  html = html.replace(/(<\/h[1-6]>|<\/pre>|<\/ul>|<\/ol>|<\/blockquote>|<\/table>|<hr \/>)\s*<\/p>/g, '$1')

  return html
})

/**
 * 生成标题 ID
 */
function generateId(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^\w\u4e00-\u9fa5]+/g, '-')
    .replace(/^-|-$/g, '')
}

/**
 * 提取目录
 */
const tocItems = computed<TocItem[]>(() => {
  if (!props.content) return []

  const items: TocItem[] = []
  const lines = props.content.split('\n')

  lines.forEach(line => {
    const match = line.match(/^(#{1,6})\s+(.+)$/)
    if (match) {
      const level = match[1].length
      if (level <= props.maxTocLevel) {
        items.push({
          id: generateId(match[2]),
          text: match[2],
          level,
        })
      }
    }
  })

  return items
})

/**
 * 滚动到指定标题
 */
function scrollToHeading(id: string) {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
</script>

<style scoped>
.markdown-renderer {
  display: flex;
  gap: 24px;
}

/* 目录 */
.markdown-toc {
  flex-shrink: 0;
  width: 200px;
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.toc-title {
  font-size: 13px;
  font-weight: 600;
  color: #525252;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.toc-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-item {
  display: block;
  font-size: 13px;
  color: #737373;
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-item:hover {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.05);
}

.toc-level-1 { padding-left: 8px; font-weight: 600; }
.toc-level-2 { padding-left: 16px; }
.toc-level-3 { padding-left: 24px; font-size: 12px; }

/* Markdown 内容 */
.markdown-content {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #404040;
}

.markdown-content :deep(h1) {
  font-size: 28px;
  font-weight: 700;
  color: #171717;
  margin: 24px 0 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.markdown-content :deep(h2) {
  font-size: 22px;
  font-weight: 600;
  color: #171717;
  margin: 20px 0 12px;
}

.markdown-content :deep(h3) {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin: 16px 0 8px;
}

.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  font-size: 16px;
  font-weight: 600;
  color: #404040;
  margin: 12px 0 8px;
}

.markdown-content :deep(p) {
  margin: 0 0 12px;
}

.markdown-content :deep(a) {
  color: #2563eb;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(strong) {
  font-weight: 600;
  color: #171717;
}

.markdown-content :deep(blockquote) {
  margin: 12px 0;
  padding: 12px 16px;
  border-left: 4px solid #3b82f6;
  background: #f8fafc;
  border-radius: 0 8px 8px 0;
  color: #525252;
}

.markdown-content :deep(pre) {
  margin: 12px 0;
  padding: 16px;
  background: #1e1e1e;
  border-radius: 10px;
  overflow-x: auto;
}

.markdown-content :deep(pre code) {
  font-size: 13px;
  line-height: 1.6;
  color: #d4d4d4;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}

.markdown-content :deep(.md-inline-code) {
  padding: 2px 6px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 13px;
  color: #dc2626;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #e5e5e5;
  margin: 20px 0;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 8px 12px;
  border: 1px solid #e5e5e5;
  text-align: left;
  font-size: 13px;
}

.markdown-content :deep(th) {
  background: #fafafa;
  font-weight: 600;
}

.markdown-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

/* 暗色模式 */
:global(.dark) .toc-title {
  color: #a3a3a3;
}

:global(.dark) .toc-item {
  color: #525252;
}

:global(.dark) .toc-item:hover {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.08);
}

:global(.dark) .markdown-content {
  color: #d4d4d4;
}

:global(.dark) .markdown-content :deep(h1),
:global(.dark) .markdown-content :deep(h2) {
  color: #fafafa;
  border-bottom-color: #262626;
}

:global(.dark) .markdown-content :deep(h3),
:global(.dark) .markdown-content :deep(h4),
:global(.dark) .markdown-content :deep(h5),
:global(.dark) .markdown-content :deep(h6) {
  color: #e5e5e5;
}

:global(.dark) .markdown-content :deep(strong) {
  color: #fafafa;
}

:global(.dark) .markdown-content :deep(blockquote) {
  background: rgba(59, 130, 246, 0.05);
  border-left-color: #60a5fa;
  color: #a3a3a3;
}

:global(.dark) .markdown-content :deep(.md-inline-code) {
  background: #333;
  color: #f87171;
}

:global(.dark) .markdown-content :deep(hr) {
  border-top-color: #262626;
}

:global(.dark) .markdown-content :deep(th),
:global(.dark) .markdown-content :deep(td) {
  border-color: #262626;
}

:global(.dark) .markdown-content :deep(th) {
  background: #1a1a1a;
}
</style>
