package com.aiagent.service;

import com.aiagent.common.PageResult;
import com.aiagent.dto.ExecutionHistoryResponseDTO;
import com.aiagent.entity.ExecutionHistory;
import com.aiagent.repository.ExecutionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionHistoryService {

    private final ExecutionHistoryRepository executionHistoryRepository;

    /**
     * 根据 Agent ID 获取执行历史（分页）
     */
    public PageResult<ExecutionHistoryResponseDTO> getHistoryByAgentId(Long agentId, int page, int size) {
        Page<ExecutionHistory> historyPage = executionHistoryRepository
                .findByAgentIdOrderByTimestampDesc(agentId, PageRequest.of(page, size));
        List<ExecutionHistoryResponseDTO> dtoList = historyPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        return PageResult.from(historyPage.map(this::toDTO));
    }

    /**
     * 根据 Agent ID 删除执行历史
     */
    @Transactional
    public void deleteHistoryByAgentId(Long agentId) {
        executionHistoryRepository.deleteByAgentId(agentId);
        log.info("已清除Agent执行历史: agentId={}", agentId);
    }

    /**
     * 保存执行历史记录
     */
    @Transactional
    public void saveExecutionHistory(Long agentId, String messageContent, String role, Long userId, Long tenantId) {
        try {
            ExecutionHistory history = new ExecutionHistory();
            history.setAgentId(agentId);
            history.setMessage(messageContent);
            history.setRole(role);
            history.setTimestamp(LocalDateTime.now());
            history.setUserId(userId);
            history.setTenantId(tenantId);
            executionHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("保存执行历史失败: {}", e.getMessage());
        }
    }

    private ExecutionHistoryResponseDTO toDTO(ExecutionHistory entity) {
        ExecutionHistoryResponseDTO dto = new ExecutionHistoryResponseDTO();
        dto.setId(entity.getId());
        dto.setAgentId(entity.getAgentId());
        dto.setTenantId(entity.getTenantId());
        dto.setUserId(entity.getUserId());
        dto.setMessage(entity.getMessage());
        dto.setRole(entity.getRole());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}
