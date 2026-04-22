package com.aiagent.service.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.agent.tool.Tool;

/**
 * LangChain4j AiServices Agent 接口定义
 * 
 * 通过声明式接口自动生成 AI Agent 实现，支持:
 * - @SystemMessage: 系统提示词
 * - @UserMessage: 用户消息模板（支持 {{variable}} 占位符）
 * - @MemoryId: 多会话记忆隔离
 * - @Tool: MCP 工具自动绑定
 * 
 * 使用方式:
 * <pre>
 *   Assistant assistant = AiServices.builder(Assistant.class)
 *       .chatLanguageModel(chatModel)
 *       .chatMemory(memory)
 *       .tools(mcpToolProvider)
 *       .build();
 *   
 *   String answer = assistant.chat("sessionId", "你好");
 * </pre>
 */
public interface AgentAssistant {

    /**
     * 基础对话（使用默认系统提示）
     */
    @SystemMessage("你是一个专业的 AI 助手，请用简洁、准确的方式回答用户问题。")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 自定义系统提示的对话
     */
    String chatWithSystemPrompt(@MemoryId String memoryId,
                                 @V("systemPrompt") String systemPrompt,
                                 @UserMessage("{{systemPrompt}}\n\n用户问题: {{userMessage}}") String userMessage);

    /**
     * 带角色设定的对话
     */
    @SystemMessage("你是一个 {{role}}，具有以下特点: {{characteristics}}。请以这个角色回答用户问题。")
    String chatAsRole(@MemoryId String memoryId,
                       @V("role") String role,
                       @V("characteristics") String characteristics,
                       @UserMessage String userMessage);

    /**
     * 结构化输出（JSON 格式）
     */
    @SystemMessage("你是一个数据提取专家。请从用户输入中提取信息，并以 JSON 格式返回。" +
            "JSON 格式: {\"intent\": \"用户意图\", \"entities\": [\"实体1\", \"实体2\"], \"confidence\": 0.95}")
    String extractStructuredData(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 代码生成
     */
    @SystemMessage("你是一个高级程序员。请根据用户需求生成高质量代码。" +
            "要求: 1. 代码简洁高效 2. 添加必要注释 3. 遵循最佳实践 4. 考虑边界情况")
    String generateCode(@MemoryId String memoryId,
                         @V("language") String language,
                         @UserMessage("使用 {{language}} 实现: {{requirement}}") String requirement);

    /**
     * 文本摘要
     */
    @SystemMessage("你是一个文本摘要专家。请对用户提供的文本进行简洁准确的摘要。")
    String summarize(@MemoryId String memoryId,
                      @V("maxLength") int maxLength,
                      @UserMessage("请将以下文本摘要为不超过 {{maxLength}} 字:\n\n{{text}}") String text);

    /**
     * 翻译
     */
    @SystemMessage("你是一个专业翻译。请准确翻译用户提供的文本，保持原文的语气和风格。")
    String translate(@MemoryId String memoryId,
                      @V("targetLanguage") String targetLanguage,
                      @UserMessage("请将以下文本翻译为 {{targetLanguage}}:\n\n{{text}}") String text);

    /**
     * 清除会话记忆
     */
    void clearMemory(@MemoryId String memoryId);
}
