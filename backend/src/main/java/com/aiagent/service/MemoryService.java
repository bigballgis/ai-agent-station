package com.aiagent.service;

import com.aiagent.entity.AgentMemory;
import com.aiagent.entity.AgentMemory.MemoryType;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentMemoryRepository;
import com.aiagent.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final AgentMemoryRepository memoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public AgentMemory createMemory(AgentMemory memory) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        memory.setTenantId(tenantId);
        return memoryRepository.save(memory);
    }

    public Page<AgentMemory> getMemories(Long agentId, String keyword, MemoryType memoryType, int page, int size) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return memoryRepository.searchMemories(agentId, tenantId, keyword, memoryType, pageable);
    }

    public AgentMemory getMemory(Long id) {
        return memoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("记忆不存在"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMemory(Long id) {
        memoryRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredMemories() {
        memoryRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void scheduledCleanup() {
        cleanupExpiredMemories();
    }

    public long countMemories(Long agentId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return memoryRepository.countByAgentIdAndTenantId(agentId, tenantId);
    }
}
