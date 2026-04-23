/**
 * useGraphSerializer - 图数据序列化/反序列化 Composable
 *
 * 从 AgentDesigner.vue 中提取的图数据导入导出逻辑：
 * - toGraphData(): 将画布节点和连接转换为后端兼容的 GraphData 格式
 * - fromGraphData(): 从 GraphData 格式恢复画布节点和连接
 * - exportJson(): 导出为 JSON 文件
 *
 * 支持新旧格式兼容（fromNodeId -> sourceId, toNodeId -> targetId）。
 */

import { computed } from 'vue'
import type { CanvasNode, Connection, GraphData } from './types'
import { getNodeTypeDefinition } from './nodeRegistry'

export function useGraphSerializer(
  nodes: { value: CanvasNode[] },
  connections: { value: Connection[] },
  agentName: { value: string },
) {
  const entryNodeId = computed(() => {
    const startNode = nodes.value.find(n => n.type === 'start')
    return startNode?.id || ''
  })

  /**
   * 将画布节点和连接转换为后端兼容的 GraphData 格式
   */
  function toGraphData(): GraphData {
    return {
      entryNodeId: entryNodeId.value,
      nodes: nodes.value.map(n => ({
        id: n.id,
        type: n.type,
        label: n.label,
        config: { ...n.config },
        position: { x: Math.round(n.x), y: Math.round(n.y) },
      })),
      connections: connections.value.map(c => ({
        sourceId: c.sourceId,
        sourcePort: c.sourcePort,
        targetId: c.targetId,
        targetPort: c.targetPort,
      })),
    }
  }

  /**
   * 从 GraphData 格式恢复画布节点和连接
   *
   * 兼容旧格式（fromNodeId -> sourceId, toNodeId -> targetId）
   */
  function fromGraphData(data: any): void {
    // 数据验证
    if (!data || !Array.isArray(data.nodes)) {
      throw new Error('无效的图数据格式：nodes必须为数组')
    }

    // 限制导入文件大小（节点数量不超过200）
    if (data.nodes.length > 200) {
      throw new Error(`节点数量超出限制：最多允许200个节点，当前${data.nodes.length}个`)
    }

    // 已知节点类型列表
    const knownNodeTypes = ['start', 'end', 'llm', 'code', 'http', 'if', 'variable', 'knowledge', 'mcp', 'template', 'memory', 'embedding']
    for (const node of data.nodes) {
      if (!knownNodeTypes.includes(node.type)) {
        throw new Error(`未知的节点类型：${node.type}`)
      }
      // 对Code节点的code字段进行长度检查（不超过10000字符）
      if (node.type === 'code' && node.config?.code && node.config.code.length > 10000) {
        throw new Error(`Code节点"${node.label || node.id}"的代码长度超出限制：最多允许10000字符`)
      }
    }

    // Handle both old (fromNodeId) and new (sourceId) formats
    nodes.value = (data.nodes || []).map((n: any) => {
      const typeDef = getNodeTypeDefinition(n.type)
      const x = n.position?.x ?? n.x ?? 100
      const y = n.position?.y ?? n.y ?? 100
      return {
        id: n.id || `node_${n.type}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
        type: n.type,
        label: n.label || typeDef?.name || n.type,
        x,
        y,
        config: n.config || typeDef?.defaultConfig || {},
        inputs: typeDef?.defaultInputs.map(p => ({ ...p })) || [],
        outputs: typeDef?.defaultOutputs.map(p => ({ ...p })) || [],
      }
    })
    connections.value = (data.connections || []).map((c: any) => ({
      id: c.id || `conn_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      sourceId: c.sourceId || c.fromNodeId,
      sourcePort: c.sourcePort || c.fromPort || 'output',
      targetId: c.targetId || c.toNodeId,
      targetPort: c.targetPort || c.toPort || 'input',
    }))
  }

  /**
   * 导出为 JSON 文件并触发下载
   */
  function exportJson() {
    const graphData = { name: agentName.value, ...toGraphData() }
    const blob = new Blob([JSON.stringify(graphData, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${agentName.value || 'agent-design'}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return {
    entryNodeId,
    toGraphData,
    fromGraphData,
    exportJson,
  }
}
