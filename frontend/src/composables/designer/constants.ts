/**
 * Designer 布局常量 - 所有组件和 composable 共享的唯一来源
 */

// 节点尺寸 (与 DesignerNode.vue CSS 保持一致)
export const NODE_WIDTH = 180
export const NODE_HEIGHT = 72

// 端口尺寸
export const PORT_RADIUS = 5
export const PORT_HIT_RADIUS = 12  // 点击热区

// 端口位置计算
export const PORT_START_Y = 28     // 第一个端口距节点顶部的距离
export const PORT_GAP = 24         // 端口间距

// 连接线
export const BEZIER_OFFSET = 80    // 贝塞尔曲线最小水平偏移

// 画布
export const GRID_SIZE_SMALL = 20  // 小网格
export const GRID_SIZE_LARGE = 100 // 大网格
export const CANVAS_SIZE = 5000    // 画布尺寸

// 自动布局
export const LAYOUT_HORIZONTAL_GAP = 280
export const LAYOUT_VERTICAL_GAP = 120

// 历史
export const MAX_HISTORY = 50

// 缩放
export const ZOOM_MIN = 0.2
export const ZOOM_MAX = 3
export const ZOOM_STEP = 0.1
export const ZOOM_WHEEL_FACTOR = 0.001
