package com.aiagent.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat_memory:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            log.debug("No chat memory found for memoryId: {}", memoryId);
            return Collections.emptyList();
        }
        try {
            List<ChatMessageWrapper> wrappers = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessageWrapper.class)
            );
            List<ChatMessage> messages = new ArrayList<>(wrappers.size());
            for (ChatMessageWrapper wrapper : wrappers) {
                messages.add(wrapper.toChatMessage());
            }
            log.debug("Retrieved {} messages for memoryId: {}", messages.size(), memoryId);
            return messages;
        } catch (Exception e) {
            log.error("Failed to deserialize chat memory for memoryId: {}", memoryId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = KEY_PREFIX + memoryId;
        try {
            List<ChatMessageWrapper> wrappers = new ArrayList<>(messages.size());
            for (ChatMessage message : messages) {
                wrappers.add(ChatMessageWrapper.from(message));
            }
            String json = objectMapper.writeValueAsString(wrappers);
            stringRedisTemplate.opsForValue().set(key, json, TTL);
            log.debug("Updated chat memory for memoryId: {} with {} messages", memoryId, messages.size());
        } catch (Exception e) {
            log.error("Failed to serialize chat memory for memoryId: {}", memoryId, e);
            throw new RuntimeException("Failed to update chat memory", e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId;
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("Deleted chat memory for memoryId: {}, result: {}", memoryId, deleted);
    }

    /**
     * Wrapper class for serializing/deserializing ChatMessage instances.
     * Uses a type discriminator to reconstruct the correct ChatMessage subtype.
     */
    public static class ChatMessageWrapper {

        private String type;
        private String text;
        private String toolName;
        private String toolExecutionResult;

        public ChatMessageWrapper() {
        }

        public static ChatMessageWrapper from(ChatMessage message) {
            ChatMessageWrapper wrapper = new ChatMessageWrapper();
            if (message instanceof SystemMessage) {
                wrapper.type = "SystemMessage";
                wrapper.text = ((SystemMessage) message).text();
            } else if (message instanceof UserMessage) {
                wrapper.type = "UserMessage";
                wrapper.text = ((UserMessage) message).singleText();
            } else if (message instanceof AiMessage) {
                wrapper.type = "AiMessage";
                AiMessage aiMessage = (AiMessage) message;
                wrapper.text = aiMessage.text();
                wrapper.toolName = aiMessage.hasToolExecutionRequests()
                        ? aiMessage.toolExecutionRequests().get(0).name()
                        : null;
            } else if (message instanceof ToolExecutionResultMessage) {
                wrapper.type = "ToolExecutionResultMessage";
                ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) message;
                wrapper.toolName = toolResult.toolName();
                wrapper.toolExecutionResult = toolResult.text();
            } else {
                wrapper.type = message.getClass().getSimpleName();
                wrapper.text = message.toString();
            }
            return wrapper;
        }

        public ChatMessage toChatMessage() {
            switch (type) {
                case "SystemMessage":
                    return SystemMessage.from(text);
                case "UserMessage":
                    return UserMessage.from(text);
                case "AiMessage":
                    if (toolName != null) {
                        return AiMessage.from(toolName);
                    }
                    return AiMessage.from(text);
                case "ToolExecutionResultMessage":
                    return ToolExecutionResultMessage.from(
                            java.util.UUID.randomUUID().toString(),
                            toolName,
                            toolExecutionResult);
                default:
                    return UserMessage.from(text);
            }
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public String getToolExecutionResult() {
            return toolExecutionResult;
        }

        public void setToolExecutionResult(String toolExecutionResult) {
            this.toolExecutionResult = toolExecutionResult;
        }
    }
}
