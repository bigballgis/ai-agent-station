/**
 * 节点类型注册表
 *
 * 集中管理所有节点类型定义，采用 Flowise 风格的插件架构。
 * 所有配置字段与后端 GraphExecutor.java 保持一致。
 *
 * 节点类型列表：start, llm, condition, tool, memory, variable,
 *              retriever, exception, http, end, code, delay, subgraph,
 *              switch, parallel, merge, human_approval
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
      // 注意：以下 provider 需要确认后端 LangChain4jService 是否已配置对应支持
      { label: 'Anthropic', value: 'anthropic' },
      { label: 'Google', value: 'google' },
      { label: 'Ollama', value: 'ollama' },
      { label: 'Azure OpenAI', value: 'azure' },
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
    type: 'slider',
    required: false,
    defaultValue: 0.7,
    min: 0,
    max: 2,
    step: 0.1,
    tooltip: '控制输出随机性，0 表示确定性输出，2 表示最大随机性',
  },
  {
    key: 'topP',
    label: 'Top P',
    type: 'slider',
    required: false,
    defaultValue: 1,
    min: 0,
    max: 1,
    step: 0.05,
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
    type: 'select',
    required: true,
    defaultValue: '',
    options: [
      { label: '请选择...', value: '' },
      { label: 'Web 搜索', value: 'web_search' },
      { label: '计算器', value: 'calculator' },
      { label: '天气查询', value: 'weather' },
      { label: '数据库查询', value: 'database' },
      { label: '邮件发送', value: 'email' },
      { label: '文件读取', value: 'file_read' },
      { label: '文件写入', value: 'file_write' },
      { label: '自定义工具', value: 'custom' },
    ],
    tooltip: '选择要调用的工具',
  },
  {
    key: 'toolName',
    label: '工具名称',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: 'my_tool',
    tooltip: '工具显示名称',
  },
  {
    key: 'inputMapping',
    label: '输入映射 (Key-Value)',
    type: 'key-value',
    required: false,
    defaultValue: [],
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
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: '来源节点/表达式',
    tooltip: '变量的来源节点或表达式',
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
    type: 'textarea',
    required: false,
    defaultValue: '',
    placeholder: '请求体...',
    tooltip: 'HTTP 请求体内容',
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

/** Subgraph/Agent Call 节点配置 Schema */
const subgraphConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'agentId',
    label: 'Agent ID',
    type: 'text',
    required: true,
    defaultValue: '',
    placeholder: '选择或输入 Agent ID',
    tooltip: '要调用的子 Agent 唯一标识',
  },
  {
    key: 'agentName',
    label: 'Agent 名称',
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: '显示名称',
    tooltip: '子 Agent 的显示名称',
  },
  {
    key: 'inputMapping',
    label: '输入映射 (JSON)',
    type: 'json',
    required: false,
    defaultValue: '{}',
    placeholder: '{"key": "{{source.output}}"}',
    tooltip: '将当前工作流变量映射到子 Agent 输入，JSON 格式',
  },
  {
    key: 'outputVariable',
    label: '输出变量名',
    type: 'text',
    required: false,
    defaultValue: 'subgraph_output',
    placeholder: 'subgraph_output',
    tooltip: '子 Agent 输出结果存储的变量名',
  },
]

/** Switch/Router 节点配置 Schema */
const switchConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'cases',
    label: '分支规则',
    type: 'switch-cases',
    required: true,
    defaultValue: [],
    tooltip: '配置条件表达式和分支标签，支持动态添加多个分支',
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

/** Human Approval 节点配置 Schema */
const humanApprovalConfigSchema: ConfigFieldSchema[] = [
  {
    key: 'approvalType',
    label: '审批类型',
    type: 'select',
    required: true,
    defaultValue: 'approve_reject',
    options: [
      { label: '批准/拒绝', value: 'approve_reject' },
      { label: '批准/拒绝/修改', value: 'approve_reject_modify' },
      { label: '需要输入', value: 'input_required' },
    ],
    tooltip: '选择审批操作类型',
  },
  {
    key: 'title',
    label: '审批标题',
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: '请审批此步骤',
    tooltip: '审批请求的标题',
  },
  {
    key: 'description',
    label: '审批说明',
    type: 'textarea',
    required: false,
    defaultValue: '',
    placeholder: '描述需要审批的内容...',
    tooltip: '详细描述需要审批的内容',
  },
  {
    key: 'approvers',
    label: '审批人',
    type: 'text',
    required: false,
    defaultValue: '',
    placeholder: '用户ID或角色，逗号分隔',
    tooltip: '指定审批人，支持用户ID或角色，逗号分隔',
  },
  {
    key: 'timeoutMinutes',
    label: '超时时间（分钟）',
    type: 'number',
    required: false,
    defaultValue: 60,
    min: 1,
    max: 10080,
    tooltip: '等待审批的超时时间，超时后执行超时处理策略',
  },
  {
    key: 'fallbackAction',
    label: '超时处理',
    type: 'select',
    required: false,
    defaultValue: 'reject',
    options: [
      { label: '自动拒绝', value: 'reject' },
      { label: '自动批准', value: 'approve' },
      { label: '升级处理', value: 'escalate' },
    ],
    tooltip: '超时后的自动处理策略',
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
// 节点类型定义（18 种，与 GraphExecutor.java 一致）
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
    configSchema: endConfigSchema,
  },
  {
    type: 'code',
    name: '代码执行',
    description: '执行自定义 JavaScript 代码（支持变量引用）',
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
    configSchema: codeConfigSchema,
  },
  {
    type: 'delay',
    name: '延迟',
    description: '暂停执行指定秒数（1-300秒）',
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
    configSchema: delayConfigSchema,
  },
  {
    type: 'subgraph',
    name: '调用 Agent',
    description: '嵌入另一个 Agent 工作流作为子图执行',
    color: '#f59e0b',
    icon: '\uD83D\uDD17',
    category: 'advanced',
    defaultConfig: {
      agentId: '',
      agentName: '',
      inputMapping: '{}',
      outputVariable: 'subgraph_output',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    configSchema: subgraphConfigSchema,
  },
  {
    type: 'switch',
    name: '多路分支',
    description: '根据条件表达式路由到多个分支（支持3+路径）',
    color: '#ec4899',
    icon: '\u2142',
    category: 'flow',
    defaultConfig: {
      cases: [
        { expression: '', label: '分支 1', outputPort: 'case_1' },
        { expression: '', label: '分支 2', outputPort: 'case_2' },
      ],
      defaultBranch: 'default',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'case_1', label: '分支 1', index: 0 },
      { name: 'case_2', label: '分支 2', index: 1 },
      { name: 'default', label: '默认', index: 2 },
    ],
    configSchema: switchConfigSchema,
  },
  {
    type: 'parallel',
    name: '并行执行',
    description: '同时执行多个分支，等待全部完成后继续',
    color: '#8b5cf6',
    icon: '\u21C9',
    category: 'flow',
    defaultConfig: {
      maxParallelism: 5,
      failStrategy: 'wait',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    configSchema: [
      { key: 'maxParallelism', label: '最大并行数', type: 'number', required: false, min: 1, max: 20, defaultValue: 5, tooltip: '最大并行执行的任务数' },
      { key: 'failStrategy', label: '失败策略', type: 'select', required: false, options: [
        { label: '等待全部完成', value: 'wait' },
        { label: '快速失败', value: 'fail_fast' },
      ], defaultValue: 'wait', tooltip: '某个分支失败时的处理策略' },
    ],
  },
  {
    type: 'merge',
    name: '合并',
    description: '合并多个分支的输出为一个结果',
    color: '#06b6d4',
    icon: '\u2442',
    category: 'flow',
    defaultConfig: {
      mergeStrategy: 'append',
      mergeExpression: '',
    },
    defaultInputs: [
      { name: 'input_1', label: '输入 1', index: 0 },
      { name: 'input_2', label: '输入 2', index: 1 },
      { name: 'input_3', label: '输入 3', index: 2 },
    ],
    defaultOutputs: [
      { name: 'output', label: '输出', index: 0 },
    ],
    configSchema: [
      { key: 'mergeStrategy', label: '合并策略', type: 'select', required: false, options: [
        { label: '追加 (Append)', value: 'append' },
        { label: '覆盖 (Overwrite)', value: 'overwrite' },
        { label: '取第一个 (First)', value: 'first' },
      ], defaultValue: 'append', tooltip: '多个分支输出的合并方式' },
    ],
  },
  {
    type: 'human_approval',
    name: '人工审批',
    description: '暂停执行等待人工审批，支持批准/拒绝/修改',
    color: '#f97316',
    icon: '\uD83D\uDC64',
    category: 'advanced',
    defaultConfig: {
      approvalType: 'approve_reject',
      title: '请审批',
      description: '',
      approvers: '',
      timeoutMinutes: 60,
      fallbackAction: 'reject',
    },
    defaultInputs: [
      { name: 'input', label: '输入', index: 0 },
    ],
    defaultOutputs: [
      { name: 'approved', label: '已批准', index: 0 },
      { name: 'rejected', label: '已拒绝', index: 1 },
    ],
    configSchema: humanApprovalConfigSchema,
  },
]

// ============================================================
// 构建注册表
// ============================================================

/**
 * 节点类型注册表
 *
 * 以 type 为 key 的映射表，包含所有节点类型的完整定义。
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
  const def = nodeTypeRegistry[type] as NodeTypeDefinition | undefined
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
