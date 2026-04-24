package com.aiagent.service;

import cn.hutool.core.util.StrUtil;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentApproval;
import com.aiagent.entity.AgentVersion;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentApprovalRepository;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
import com.aiagent.tenant.TenantContextHolder;
import com.aiagent.websocket.WebSocketEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApprovalService {

    private final AgentApprovalRepository agentApprovalRepository;
    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentTestResultService testResultService;
    private final NotificationService notificationService;

    public Page<AgentApproval> getApprovals(Pageable pageable) {
        Long tenantId = TenantContextHolder.getTenantId();
        return agentApprovalRepository.findByTenantId(tenantId, pageable);
    }

    public Page<AgentApproval> getPendingApprovals(Pageable pageable) {
        Long tenantId = TenantContextHolder.getTenantId();
        return agentApprovalRepository.findByTenantIdAndStatus(tenantId, AgentApproval.ApprovalStatus.PENDING, pageable);
    }

    public AgentApproval getApprovalById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        return agentApprovalRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("审批记录不存在"));
    }

    public List<AgentApproval> getApprovalsByAgentId(Long agentId) {
        Long tenantId = TenantContextHolder.getTenantId();
        return agentApprovalRepository.findByAgentIdAndTenantId(agentId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentApproval submitForApproval(Long agentId, Long versionId, String remark, Long submitterId) {
        Long tenantId = TenantContextHolder.getTenantId();

        Agent agent = agentRepository.findByIdAndTenantId(agentId, tenantId)
                .orElseThrow(() -> new BusinessException("Agent不存在"));

        if (agent.getStatus() != Agent.AgentStatus.DRAFT && agent.getStatus() != Agent.AgentStatus.APPROVED) {
            throw new BusinessException("当前状态不允许提交审批");
        }

        // 检查测试状态
        double passRate = testResultService.getPassRateByAgent(agentId);
        if (passRate < 80) {
            throw new BusinessException("测试通过率低于80%，不允许提交审批");
        }

        AgentVersion version = agentVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("版本不存在"));

        AgentApproval approval = new AgentApproval();
        approval.setAgentId(agentId);
        approval.setTenantId(tenantId);
        approval.setAgentVersionId(versionId);
        approval.setSubmitterId(submitterId);
        approval.setRemark(remark);
        approval.setStatus(AgentApproval.ApprovalStatus.PENDING);
        approval.setSubmittedAt(LocalDateTime.now());

        agent.setStatus(Agent.AgentStatus.PENDING_APPROVAL);
        agentRepository.save(agent);

        AgentApproval saved = agentApprovalRepository.save(approval);

        // Publish real-time APPROVAL_PENDING event via WebSocket
        try {
            notificationService.broadcastEvent(
                    WebSocketEventDTO.approvalPending(
                            agent.getName(),
                            saved.getId(),
                            String.valueOf(submitterId)
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to publish approval pending event: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentApproval approve(Long approvalId, String approvalRemark, Long approverId) {
        Long tenantId = TenantContextHolder.getTenantId();

        AgentApproval approval = agentApprovalRepository.findByIdAndTenantId(approvalId, tenantId)
                .orElseThrow(() -> new BusinessException("审批记录不存在"));

        if (approval.getStatus() != AgentApproval.ApprovalStatus.PENDING) {
            throw new BusinessException("当前状态不允许审批");
        }

        approval.setStatus(AgentApproval.ApprovalStatus.APPROVED);
        approval.setApproverId(approverId);
        approval.setApprovalRemark(approvalRemark);
        approval.setApprovedAt(LocalDateTime.now());

        Agent agent = agentRepository.findByIdAndTenantId(approval.getAgentId(), tenantId)
                .orElseThrow(() -> new BusinessException("Agent不存在"));
        agent.setStatus(Agent.AgentStatus.APPROVED);
        agentRepository.save(agent);

        return agentApprovalRepository.save(approval);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentApproval reject(Long approvalId, String approvalRemark, Long approverId) {
        Long tenantId = TenantContextHolder.getTenantId();

        AgentApproval approval = agentApprovalRepository.findByIdAndTenantId(approvalId, tenantId)
                .orElseThrow(() -> new BusinessException("审批记录不存在"));

        if (approval.getStatus() != AgentApproval.ApprovalStatus.PENDING) {
            throw new BusinessException("当前状态不允许审批");
        }

        approval.setStatus(AgentApproval.ApprovalStatus.REJECTED);
        approval.setApproverId(approverId);
        approval.setApprovalRemark(approvalRemark);
        approval.setApprovedAt(LocalDateTime.now());

        Agent agent = agentRepository.findByIdAndTenantId(approval.getAgentId(), tenantId)
                .orElseThrow(() -> new BusinessException("Agent不存在"));
        agent.setStatus(Agent.AgentStatus.DRAFT);
        agentRepository.save(agent);

        return agentApprovalRepository.save(approval);
    }
}
