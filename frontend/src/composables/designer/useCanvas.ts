/**
 * useCanvas - 画布状态管理 Composable
 *
 * 管理画布的缩放、平移、适配视口和网格显示。
 * 从 AgentDesigner.vue 中提取的画布交互逻辑。
 *
 * 功能：
 * - 响应式缩放（0.2 ~ 3）和平移（panX, panY）
 * - canvasTransformStyle 计算属性（translate + scale）
 * - gridOffsetStyle 计算属性（网格背景偏移）
 * - zoomIn / zoomOut / resetZoom 缩放方法
 * - fitView(nodes, containerRef) 自动适配所有节点到视口
 * - handleWheel(event) 鼠标滚轮缩放（朝鼠标位置缩放）
 * - startPan / endPan / updatePan 画布拖拽平移
 */

import { ref, computed, type Ref } from 'vue'
import type { CanvasNode } from './types'
import { NODE_WIDTH, NODE_HEIGHT, ZOOM_MIN, ZOOM_MAX, ZOOM_STEP } from './constants'

/** 缩放默认值 */
const ZOOM_DEFAULT = 1

/** fitView 内边距 */
const FIT_PADDING = 60

export function useCanvas() {
  // ============================================================
  // 响应式状态
  // ============================================================

  /** 缩放比例 */
  const zoom = ref<number>(ZOOM_DEFAULT)

  /** X 轴平移量 */
  const panX = ref<number>(0)

  /** Y 轴平移量 */
  const panY = ref<number>(0)

  /** 是否正在拖拽画布 */
  const isPanning = ref<boolean>(false)

  /** 拖拽起始鼠标位置 */
  const panStartX = ref<number>(0)

  /** 拖拽起始鼠标位置 */
  const panStartY = ref<number>(0)

  /** 拖拽起始时的 panX */
  const panStartPanX = ref<number>(0)

  /** 拖拽起始时的 panY */
  const panStartPanY = ref<number>(0)

  // ============================================================
  // 计算属性
  // ============================================================

  /**
   * 画布变换样式
   *
   * 用于 SVG/HTML 容器的 transform 属性，实现缩放和平移。
   */
  const canvasTransformStyle = computed<string>(() => {
    return `translate(${panX.value}px, ${panY.value}px) scale(${zoom.value})`
  })

  /**
   * 网格偏移样式
   *
   * 用于网格背景的 background-position，使网格随画布移动。
   */
  const gridOffsetStyle = computed<string>(() => {
    const gridSize = 20 * zoom.value
    return `${panX.value % gridSize}px ${panY.value % gridSize}px`
  })

  // ============================================================
  // 缩放方法
  // ============================================================

  /**
   * 放大画布
   */
  function zoomIn(): void {
    zoom.value = Math.min(ZOOM_MAX, Math.round((zoom.value + ZOOM_STEP) * 100) / 100)
  }

  /**
   * 缩小画布
   */
  function zoomOut(): void {
    zoom.value = Math.max(ZOOM_MIN, Math.round((zoom.value - ZOOM_STEP) * 100) / 100)
  }

  /**
   * 重置缩放和平移到默认状态
   */
  function resetZoom(): void {
    zoom.value = ZOOM_DEFAULT
    panX.value = 0
    panY.value = 0
  }

  /**
   * 设置缩放值
   *
   * @param value - 目标缩放值，会被限制在 [ZOOM_MIN, ZOOM_MAX] 范围内
   */
  function setZoom(value: number): void {
    zoom.value = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, value))
  }

  // ============================================================
  // 适配视口
  // ============================================================

  /**
   * 自动适配所有节点到视口
   *
   * 计算所有节点的边界框，然后调整缩放和平移使所有节点可见。
   *
   * @param nodes - 当前画布上的节点列表
   * @param containerRef - 画布容器的 DOM 引用
   */
  function fitView(nodes: CanvasNode[], containerRef: Ref<HTMLElement | null>): void {
    const container = containerRef.value
    if (!container || nodes.length === 0) {
      resetZoom()
      return
    }

    const containerWidth = container.clientWidth
    const containerHeight = container.clientHeight

    if (containerWidth === 0 || containerHeight === 0) {
      return
    }

    // 计算所有节点的边界框
    let minX = Infinity
    let minY = Infinity
    let maxX = -Infinity
    let maxY = -Infinity

    for (const node of nodes) {
      minX = Math.min(minX, node.x)
      minY = Math.min(minY, node.y)
      maxX = Math.max(maxX, node.x + NODE_WIDTH)
      maxY = Math.max(maxY, node.y + NODE_HEIGHT)
    }

    const graphWidth = maxX - minX
    const graphHeight = maxY - minY

    // 计算适配缩放比例
    const scaleX = (containerWidth - FIT_PADDING * 2) / graphWidth
    const scaleY = (containerHeight - FIT_PADDING * 2) / graphHeight
    const newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, Math.min(scaleX, scaleY)))

    // 计算居中平移
    const graphCenterX = minX + graphWidth / 2
    const graphCenterY = minY + graphHeight / 2
    const newPanX = containerWidth / 2 - graphCenterX * newZoom
    const newPanY = containerHeight / 2 - graphCenterY * newZoom

    zoom.value = Math.round(newZoom * 100) / 100
    panX.value = Math.round(newPanX)
    panY.value = Math.round(newPanY)
  }

  // ============================================================
  // 鼠标滚轮缩放
  // ============================================================

  /**
   * 处理鼠标滚轮事件，朝鼠标位置缩放
   *
   * @param event - 鼠标滚轮事件
   * @param containerRef - 画布容器的 DOM 引用
   */
  function handleWheel(
    event: WheelEvent,
    containerRef: Ref<HTMLElement | null>,
  ): void {
    event.preventDefault()

    const container = containerRef.value
    if (!container) return

    const rect = container.getBoundingClientRect()

    // 鼠标在容器中的位置
    const mouseX = event.clientX - rect.left
    const mouseY = event.clientY - rect.top

    // 缩放前的画布坐标
    const beforeX = (mouseX - panX.value) / zoom.value
    const beforeY = (mouseY - panY.value) / zoom.value

    // 计算新缩放值
    const delta = event.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP
    const newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom.value + delta))

    // 调整平移使鼠标位置下的画布坐标不变
    panX.value = mouseX - beforeX * newZoom
    panY.value = mouseY - beforeY * newZoom
    zoom.value = Math.round(newZoom * 100) / 100
  }

  // ============================================================
  // 画布拖拽平移
  // ============================================================

  /**
   * 开始拖拽画布
   *
   * @param event - 鼠标按下事件
   * @param containerRef - 画布容器的 DOM 引用
   */
  function startPan(
    event: MouseEvent,
    containerRef: Ref<HTMLElement | null>,
  ): void {
    const container = containerRef.value
    if (!container) return

    isPanning.value = true
    panStartX.value = event.clientX
    panStartY.value = event.clientY
    panStartPanX.value = panX.value
    panStartPanY.value = panY.value
  }

  /**
   * 更新拖拽位置
   *
   * @param event - 鼠标移动事件
   */
  function updatePan(event: MouseEvent): void {
    if (!isPanning.value) return

    const dx = event.clientX - panStartX.value
    const dy = event.clientY - panStartY.value

    panX.value = panStartPanX.value + dx
    panY.value = panStartPanY.value + dy
  }

  /**
   * 结束拖拽
   */
  function endPan(): void {
    isPanning.value = false
  }

  // ============================================================
  // 坐标转换
  // ============================================================

  /**
   * 屏幕坐标转画布坐标
   *
   * @param screenX - 屏幕 X 坐标
   * @param screenY - 屏幕 Y 坐标
   * @param containerRef - 画布容器的 DOM 引用
   * @returns 画布坐标 {x, y}
   */
  function screenToCanvas(
    screenX: number,
    screenY: number,
    containerRef: Ref<HTMLElement | null>,
  ): { x: number; y: number } {
    const container = containerRef.value
    if (!container) return { x: 0, y: 0 }

    const rect = container.getBoundingClientRect()
    const mouseX = screenX - rect.left
    const mouseY = screenY - rect.top

    return {
      x: (mouseX - panX.value) / zoom.value,
      y: (mouseY - panY.value) / zoom.value,
    }
  }

  return {
    // 响应式状态
    zoom,
    panX,
    panY,
    isPanning,

    // 计算属性
    canvasTransformStyle,
    gridOffsetStyle,

    // 缩放方法
    zoomIn,
    zoomOut,
    resetZoom,
    setZoom,

    // 视口适配
    fitView,

    // 鼠标滚轮缩放
    handleWheel,

    // 画布拖拽
    startPan,
    updatePan,
    endPan,

    // 坐标转换
    screenToCanvas,
  }
}
