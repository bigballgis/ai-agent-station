package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StreamService {

    private final AgentRepository agentRepository;

    public Agent getAgentById(Long agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent不存在: " + agentId));
    }
}
