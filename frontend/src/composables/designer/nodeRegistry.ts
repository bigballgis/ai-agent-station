/**
 * 节点类型注册表
 *
 * 集中管理所有 12 种节点类型定义，采用 Flowise 风格的插件架构。
 * 所有配置字段与后端 GraphExecutor.java 保持一致。
 *
 * 节点类型列表：start, llm, condition, tool, memory, variable,
 *              retriever, exception, http, end, code, delay
 */

import type {
  NodeTypeDefinition,
  NodeRegistry,
  ConfigFieldSchema,
} from './types'

// ============================================================
// 各节点类型的配置 Schema（用于 ConfigPanel 渲染）
// ============================================================

/** Start 节点配置 Schema */
const startConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'inputVariables',
    label: '输入变量',
    type: 'json',
    required: false,
    defaultValue: '{}',
    placeholder: '{"key": "value"}',
    tooltip: '定义工作流的输入变量，JSON 格式',
  },
]

/** LLM 节点配置 Schema */
const llmConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'provider',
    label: '模型提供商',
    type: 'select',
    required: true,
    defaultValue: 'openai',
    options: [
      { label: 'OpenAI', value: 'openai' },
      { label: 'Anthropic', value: 'anthropic' },
      { label: 'Azure OpenAI', value: 'azure' },
      { label: 'Ollama', value: 'ollama' },
    ],
    tooltip: '选择 LLM 服务提供商',
  },
  {
    key: 'model',
    label: '模型名称',
    type: 'text',
    required: true,
    defaultValue: 'gpt-4',
    placeholder: 'gpt-4 / claude-3-opus / llama3',
    tooltip: '模型标识符，如 gpt-4、claude-3-opus 等',
  },
  {
    key: 'temperature',
    label: '温度',
    type: 'number',
    required: false,
    defaultValue: 0.7,
    min: 0,
    max: 2,
    tooltip: '控制输出随机性，0 表示确定性输出，2 表示最大随机性',
  },
  {
    key: 'topP',
    label: 'Top P',
    type: 'number',
    required: false,
    defaultValue: 1,
    min: 0,
    max: 1,
    tooltip: '核采样参数，控制候选词范围',
  },
  {
    key: 'maxTokens',
    label: '最大 Token 数',
    type: 'number',
    required: false,
    defaultValue: 2048,
    min: 1,
    max: 128000,
    tooltip: '生成的最大 token 数量',
  },
  {
    key: 'systemPrompt',
    label: '系统提示词',
    type: 'textarea',
    required: false,
    defaultValue: '',
    placeholder: '你是一个有帮助的助手...',
    tooltip: '设定 AI 角色和行为的系统级指令',
  },
  {
    key: 'prompt',
    label: '用户提示词',
    type: 'textarea',
    required: true,
    defaultValue: '',
    placeholder: '请根据以下信息回答：{{input}}',
    tooltip: '发送给 LLM 的用户提示词，支持 {{变量}} 模板语法',
  },
]

/** Condition 节点配置 Schema */
const conditionConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'expression',
    label: '条件表达式',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: '{{output}} == "expected"',
    tooltip: '条件判断表达式，支持 {{变量}} 引用',
  },
  {
    key: 'variable',
    label: '判断变量',
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: 'output',
    tooltip: '用于条件判断的变量名',
  },
]

/** Tool 节点配置 Schema */
const toolConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'toolId',
    label: '工具 ID',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: 'tool_001',
    tooltip: '关联的工具唯一标识',
  },
  {
    key: 'toolName',
    label: '工具名称',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: 'Web Search',
    tooltip: '工具显示名称',
  },
  {
    key: 'inputMapping',
    label: '输入映射',
    type: 'json',
    required: false,
    defaultValue: '[]',
    placeholder: '[{"key": "query", "value": "{{input}}"}]',
    tooltip: '将工作流变量映射到工具输入参数',
  },
]

/** Memory 节点配置 Schema */
const memoryConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'action',
    label: '操作类型',
    type: 'select',
    required: true,
    defaultValue: 'load',
    options: [
      { label: '加载记忆', value: 'load' },
      { label: '保存记忆', value: 'save' },
    ],
    tooltip: '选择加载或保存记忆',
  },
  {
    key: 'memoryType',
    label: '记忆类型',
    type: 'select',
    required: true,
    defaultValue: 'SHORT_TERM',
    options: [
      { label: '短期记忆', value: 'SHORT_TERM' },
      { label: '长期记忆', value: 'LONG_TERM' },
      { label: '业务记忆', value: 'BUSINESS' },
    ],
    tooltip: '选择记忆存储类型',
  },
  {
    key: 'query',
    label: '查询内容',
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: '{{input}}',
    tooltip: '用于检索记忆的查询内容',
  },
  {
    key: 'summary',
    label: '摘要内容',
    type: 'textarea',
    required: false,
    defaultValue: '',
    placeholder: '保存到记忆中的摘要...',
    tooltip: '保存记忆时写入的摘要内容',
  },
]

/** Variable 节点配置 Schema */
const variableConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'name',
    label: '变量名',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: 'myVariable',
    tooltip: '变量名称，用于在工作流中引用',
  },
  {
    key: 'value',
    label: '变量值',
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: '变量值或 {{引用}}',
    tooltip: '变量的值，支持 {{变量}} 模板语法',
  },
  {
    key: 'source',
    label: '变量来源',
    type: 'select',
    required: false,
    defaultValue: 'static',
    options: [
      { label: '静态值', value: 'static' },
      { label: '上游输出', value: 'upstream' },
      { label: '环境变量', value: 'env' },
    ],
    tooltip: '变量的来源方式',
  },
]

/** Retriever 节点配置 Schema */
const retrieverConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'query',
    label: '检索查询',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: '{{input}}',
    tooltip: '用于检索的查询文本',
  },
  {
    key: 'retrieverType',
    label: '检索器类型',
    type: 'select',
    required: true,
    defaultValue: 'memory',
    options: [
      { label: '记忆检索', value: 'memory' },
      { label: '向量数据库', value: 'vector_db' },
    ],
    tooltip: '选择检索数据源',
  },
]

/** Exception 节点配置 Schema */
const exceptionConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'action',
    label: '异常处理方式',
    type: 'select',
    required: true,
    defaultValue: 'log',
    options: [
      { label: '记录日志', value: 'log' },
      { label: '重试', value: 'retry' },
      { label: '降级处理', value: 'fallback' },
    ],
    tooltip: '选择异常发生时的处理策略',
  },
  {
    key: 'fallbackValue',
    label: '降级值',
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: '默认返回值',
    tooltip: '降级处理时返回的默认值',
  },
]

/** HTTP 节点配置 Schema */
const httpConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'url',
    label: '请求 URL',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: 'https://api.example.com/data',
    tooltip: 'HTTP 请求地址',
  },
  {
    key: 'method',
    label: '请求方法',
    type: 'select',
    required: true,
    defaultValue: 'GET',
    options: [
      { label: 'GET', value: 'GET' },
      { label: 'POST', value: 'POST' },
      { label: 'PUT', value: 'PUT' },
      { label: 'DELETE', value: 'DELETE' },
    ],
    tooltip: 'HTTP 请求方法',
  },
  {
    key: 'headers',
    label: '请求头',
    type: 'json',
    required: false,
    defaultValue: '{}',
    placeholder: '{"Content-Type": "application/json"}',
    tooltip: '自定义 HTTP 请求头，JSON 格式',
  },
  {
    key: 'body',
    label: '请求体',
    type: 'json',
    required: false,
    defaultValue: '',
    placeholder: '{"key": "value"}',
    tooltip: 'HTTP 请求体内容，JSON 格式',
  },
]

/** Code 节点配置 Schema */
const codeConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'language',
    label: '编程语言',
    type: 'select',
    required: true,
    defaultValue: 'javascript',
    options: [
      { label: 'JavaScript', value: 'javascript' },
      { label: 'Python', value: 'python' },
    ],
    tooltip: '代码执行的编程语言',
  },
  {
    key: 'code',
    label: '代码内容',
    type: 'code',
    required: true,
    defaultValue: '',
    placeholder: '// 在此编写代码\nreturn input;',
    tooltip: '自定义代码逻辑，通过 return 返回结果',
  },
]

/** Delay 节点配置 Schema */
const delayConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'seconds',
    label: '延迟秒数',
    type: 'number',
    required: true,
    defaultValue: 1,
    min: 0.1,
    max: 3600,
    tooltip: '延迟等待的秒数',
  },
]

/** End 节点配置 Schema */
const endConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'outputFormat',
    label: '输出格式',
    type: 'select',
    required: false,
    defaultValue: 'text',
    options: [
      { label: '纯文本', value: 'text' },
      { label: 'JSON', value: 'json' },
      { label: 'Markdown', value: 'markdown' },
    ],
    tooltip: '最终输出的格式',
  },
  {
    key: 'outputTemplate',
    label: '输出模板',
    type: 'textarea',
    required: false,
    defaultValue: '',
    placeholder: '结果：{{output}}',
    tooltip: '输出模板，支持 {{变量}} 模板语法',
  },
]

// ============================================================
// 节点类型定义（12 种，与 GraphExecutor.java 一致）
// ============================================================

const nodeTypeDefinitions: NodeTypeDefinition[] = [
  {
    type: 'start',
    name: '开始',
    description: '工作流入口节点，定义输入变量并启动执行流程',
    color: '#10b981',
    icon: '\u25B6',
    category: 'flow',
    defaultConfig: {
      inputVariables: '{}',
    },
    defaultInputs: [],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: startConfigSchema,
  },
  {
    type: 'llm',
    name: '大语言模型',
    description: '调用大语言模型进行文本生成、对话、摘要等 AI 任务',
    color: '#6366f1',
    icon: '\uD83E\uDD16',
    category: 'ai',
    defaultConfig: {
      provider: 'openai',
      model: 'gpt-4',
      temperature: 0.7,
      topP: 1,
      maxTokens: 2048,
      systemPrompt: '',
      prompt: '',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: llmConfigSchema,
  },
  {
    type: 'condition',
    name: '条件判断',
    description: '根据条件表达式进行分支路由，支持 true/false 两路输出',
    color: '#f59e0b',
    icon: '\u2753',
    category: 'flow',
    defaultConfig: {
      expression: '',
      variable: '',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'true', label: 'True', index: 0 },
      { name: 'false', label: 'False', index: 1 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: conditionConfigSchema,
  },
  {
    type: 'tool',
    name: '工具调用',
    description: '调用外部工具或 API 执行特定操作，如搜索、计算等',
    color: '#8b5cf6',
    icon: '\uD83D\uDD27',
    category: 'integration',
    defaultConfig: {
      toolId: '',
      toolName: '',
      inputMapping: [],
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: toolConfigSchema,
  },
  {
    type: 'memory',
    name: '记忆',
    description: '加载或保存对话记忆，支持短期、长期和业务记忆',
    color: '#06b6d4',
    icon: '\uD83D\uDCBE',
    category: 'ai',
    defaultConfig: {
      action: 'load',
      memoryType: 'SHORT_TERM',
      query: '',
      summary: '',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: memoryConfigSchema,
  },
  {
    type: 'variable',
    name: '变量',
    description: '定义或引用变量，支持静态值、上游输出和环境变量',
    color: '#84cc16',
    icon: '\uD83D\uDCDD',
    category: 'flow',
    defaultConfig: {
      name: '',
      value: '',
      source: 'static',
    },
    defaultInputs: [],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: variableConfigSchema,
  },
  {
    type: 'retriever',
    name: '检索器',
    description: '从记忆或向量数据库中检索相关内容',
    color: '#0ea5e9',
    icon: '\uD83D\uDD0D',
    category: 'ai',
    defaultConfig: {
      query: '',
      retrieverType: 'memory',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: retrieverConfigSchema,
  },
  {
    type: 'exception',
    name: '异常处理',
    description: '捕获和处理工作流执行中的异常，支持日志、重试和降级',
    color: '#ef4444',
    icon: '\u26A0',
    category: 'advanced',
    defaultConfig: {
      action: 'log',
      fallbackValue: '',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
      { name: 'error', label: '错误', index: 1 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: exceptionConfigSchema,
  },
  {
    type: 'http',
    name: 'HTTP 请求',
    description: '发送 HTTP/HTTPS 请求调用外部 API',
    color: '#f97316',
    icon: '\uD83C\uDF10',
    category: 'integration',
    defaultConfig: {
      url: '',
      method: 'GET',
      headers: {},
      body: '',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: httpConfigSchema,
  },
  {
    type: 'end',
    name: '结束',
    description: '工作流结束节点，定义最终输出格式',
    color: '#64748b',
    icon: '\u23F9',
    category: 'flow',
    defaultConfig: {
      outputFormat: 'text',
      outputTemplate: '',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: endConfigSchema,
  },
  {
    type: 'code',
    name: '代码执行',
    description: '执行自定义 JavaScript 或 Python 代码',
    color: '#a855f7',
    icon: '\uD83D\uDCBB',
    category: 'advanced',
    defaultConfig: {
      language: 'javascript',
      code: '',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: codeConfigSchema,
  },
  {
    type: 'delay',
    name: '延迟',
    description: '暂停工作流执行指定秒数',
    color: '#78716c',
    icon: '\u23F3',
    category: 'advanced',
    defaultConfig: {
      seconds: 1,
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    // @ts-expect-error configSchema 不在基础接口中，作为扩展字段
    configSchema: delayConfigSchema,
  },
]

// ============================================================
// 构建注册表
// ============================================================

/**
 * 节点类型注册表
 *
 * 以 type 为 key 的映射表，包含所有 12 种节点类型的完整定义。
 */
export const nodeTypeRegistry: NodeRegistry = nodeTypeDefinitions.reduce(
  (registry, def) => {
    registry[def.type] = def
    return registry
  },
  {} as NodeRegistry,
)

/**
 * 获取节点类型定义
 *
 * @param type - 节点类型标识
 * @returns 节点类型定义，如果不存在则返回 undefined
 */
export function getNodeTypeDefinition(type: string): NodeTypeDefinition | undefined {
  return nodeTypeRegistry[type]
}

/**
 * 获取节点颜色
 *
 * @param type - 节点类型标识
 * @returns 节点颜色值，默认返回 '#64748b'
 */
export function getNodeColor(type: string): string {
  return nodeTypeRegistry[type]?.color ?? '#64748b'
}

/**
 * 获取节点图标
 *
 * @param type - 节点类型标识
 * @returns 节点图标 emoji，默认返回 '?'
 */
export function getNodeIcon(type: string): string {
  return nodeTypeRegistry[type]?.icon ?? '?'
}

/**
 * 获取所有节点类型定义列表
 *
 * @returns 所有节点类型定义数组
 */
export function getAllNodeTypes(): NodeTypeDefinition[] {
  return nodeTypeDefinitions
}

/**
 * 按分类获取节点类型
 *
 * @param category - 节点分类
 * @returns 该分类下的节点类型定义数组
 */
export function getNodeTypesByCategory(
  category: 'flow' | 'ai' | 'integration' | 'advanced',
): NodeTypeDefinition[] {
  return nodeTypeDefinitions.filter((def) => def.category === category)
}

/**
 * 获取节点配置 Schema
 *
 * @param type - 节点类型标识
 * @returns 配置字段 Schema 数组，如果不存在则返回空数组
 */
export function getConfigSchema(type: string): ConfigFieldSchema[] {
  const def = nodeTypeRegistry[type] as any
  return def?.configSchema ?? []
}

/**
 * 节点类型名称（中文）映射
 */
export const nodeTypeNames: Record<string, string> = nodeTypeDefinitions.reduce(
  (map, def) => {
    map[def.type] = def.name
    return map
  },
  {} as Record<string, string>,
)
