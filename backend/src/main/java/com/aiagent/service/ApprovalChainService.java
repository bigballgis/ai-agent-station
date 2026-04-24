package com.aiagent.service;

import com.aiagent.entity.ApprovalChain;
import com.aiagent.entity.ApprovalChainStep;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.ApprovalChainRepository;
import com.aiagent.repository.ApprovalChainStepRepository;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalChainService {

    private final ApprovalChainRepository chainRepository;
    private final ApprovalChainStepRepository stepRepository;

    /**
     * Create a new approval chain definition
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalChain createChain(ApprovalChain chain) {
        Long tenantId = TenantContextHolder.getTenantId();
        chain.setTenantId(tenantId);
        chain.setStatus(ApprovalChain.ChainStatus.ACTIVE);
        return chainRepository.save(chain);
    }

    /**
     * Get all chains for current tenant
     */
    public List<ApprovalChain> getAllChains() {
        Long tenantId = TenantContextHolder.getTenantId();
        return chainRepository.findByTenantId(tenantId);
    }

    /**
     * Get chains with pagination
     */
    public Page<ApprovalChain> getChains(Pageable pageable) {
        Long tenantId = TenantContextHolder.getTenantId();
        return chainRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get chain by ID
     */
    public ApprovalChain getChainById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        return chainRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("审批链不存在"));
    }

    /**
     * Start an approval process for a given chain and approval ID
     * Creates step records based on the chain definition
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ApprovalChainStep> startApproval(Long chainId, Long approvalId) {
        Long tenantId = TenantContextHolder.getTenantId();

        ApprovalChain chain = chainRepository.findByIdAndTenantId(chainId, tenantId)
                .orElseThrow(() -> new BusinessException("审批链不存在"));

        if (chain.getStatus() != ApprovalChain.ChainStatus.ACTIVE) {
            throw new BusinessException("审批链未激活");
        }

        if (chain.getSteps() == null) {
            throw new BusinessException("审批链没有步骤定义");
        }

        // Parse steps from chain definition and create step records
        // JSON 反序列化后审批链步骤是 Object 类型，需要强制转换为 List<Map>
        @SuppressWarnings("unchecked")

        if (stepDefinitions == null || stepDefinitions.isEmpty()) {
            throw new BusinessException("审批链步骤定义为空");
        }

        java.util.ArrayList<ApprovalChainStep> createdSteps = new java.util.ArrayList<>();

        for (int i = 0; i < stepDefinitions.size(); i++) {
            Map<String, Object> stepDef = stepDefinitions.get(i);

            ApprovalChainStep step = new ApprovalChainStep();
            step.setChainId(chainId);
            step.setApprovalId(approvalId);
            step.setStepLevel(i + 1);
            step.setStepName((String) stepDef.getOrDefault("name", "步骤 " + (i + 1)));
            step.setApproverType((String) stepDef.getOrDefault("approverType", "USER"));
            step.setApproverId(stepDef.get("approverId") != null ? ((Number) stepDef.get("approverId")).longValue() : null);
            step.setApproverName((String) stepDef.get("approverName"));
            step.setStatus(ApprovalChainStep.StepStatus.PENDING);

            createdSteps.add(stepRepository.save(step));
        }

        log.info("Started approval chain: chainId={}, approvalId={}, steps={}", chainId, approvalId, createdSteps.size());
        return createdSteps;
    }

    /**
     * Approve the current pending step
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalChainStep approveStep(Long stepId, Long userId, String comment) {
        ApprovalChainStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException("审批步骤不存在"));

        if (step.getStatus() != ApprovalChainStep.StepStatus.PENDING) {
            throw new BusinessException("当前步骤不在待审批状态");
        }

        // Verify the user is the designated approver
        if (step.getApproverId() != null && !step.getApproverId().equals(userId)) {
            throw new BusinessException("您不是当前步骤的审批人");
        }

        step.setStatus(ApprovalChainStep.StepStatus.APPROVED);
        step.setComment(comment);
        step.setActedAt(LocalDateTime.now());
        stepRepository.save(step);

        // Auto-advance: check if there is a next step and activate it
        autoAdvanceToNextStep(step);

        log.info("Approved step: stepId={}, approvalId={}, level={}", stepId, step.getApprovalId(), step.getStepLevel());
        return step;
    }

    /**
     * Reject the current pending step
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalChainStep rejectStep(Long stepId, Long userId, String comment) {
        ApprovalChainStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException("审批步骤不存在"));

        if (step.getStatus() != ApprovalChainStep.StepStatus.PENDING) {
            throw new BusinessException("当前步骤不在待审批状态");
        }

        if (step.getApproverId() != null && !step.getApproverId().equals(userId)) {
            throw new BusinessException("您不是当前步骤的审批人");
        }

        step.setStatus(ApprovalChainStep.StepStatus.REJECTED);
        step.setComment(comment);
        step.setActedAt(LocalDateTime.now());
        stepRepository.save(step);

        // Skip all subsequent steps when rejected
        skipSubsequentSteps(step);

        log.info("Rejected step: stepId={}, approvalId={}, level={}", stepId, step.getApprovalId(), step.getStepLevel());
        return step;
    }

    /**
     * Get the current pending step for an approval
     */
    public ApprovalChainStep getCurrentStep(Long approvalId) {
        List<ApprovalChainStep> pendingSteps = stepRepository.findByApprovalIdAndStatus(approvalId, ApprovalChainStep.StepStatus.PENDING);
        if (pendingSteps.isEmpty()) {
            return null;
        }
        // Return the step with the lowest level
        return pendingSteps.stream()
                .min((a, b) -> Integer.compare(a.getStepLevel(), b.getStepLevel()))
                .orElse(null);
    }

    /**
     * Get all steps for an approval (progress view)
     */
    public List<ApprovalChainStep> getApprovalProgress(Long approvalId) {
        return stepRepository.findByApprovalIdOrderByStepLevelAsc(approvalId);
    }

    /**
     * Check if all steps in an approval chain are approved
     */
    public boolean isFullyApproved(Long approvalId) {
        List<ApprovalChainStep> steps = stepRepository.findByApprovalIdOrderByStepLevelAsc(approvalId);
        if (steps.isEmpty()) {
            return false;
        }

        // Check if any step is still pending or rejected
        return steps.stream().allMatch(s -> s.getStatus() == ApprovalChainStep.StepStatus.APPROVED
                || s.getStatus() == ApprovalChainStep.StepStatus.SKIPPED);
    }

    /**
     * Check if approval has been rejected at any step
     */
    public boolean isRejected(Long approvalId) {
        List<ApprovalChainStep> steps = stepRepository.findByApprovalIdOrderByStepLevelAsc(approvalId);
        return steps.stream().anyMatch(s -> s.getStatus() == ApprovalChainStep.StepStatus.REJECTED);
    }

    /**
     * Update chain definition
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalChain updateChain(Long id, ApprovalChain chainDetails) {
        ApprovalChain chain = getChainById(id);

        if (chain.getName() != null) {
            chain.setName(chainDetails.getName());
        }
        if (chainDetails.getDescription() != null) {
            chain.setDescription(chainDetails.getDescription());
        }
        if (chainDetails.getSteps() != null) {
            chain.setSteps(chainDetails.getSteps());
        }
        if (chainDetails.getStatus() != null) {
            chain.setStatus(chainDetails.getStatus());
        }

        return chainRepository.save(chain);
    }

    /**
     * Delete chain
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteChain(Long id) {
        ApprovalChain chain = getChainById(id);
        chain.setStatus(ApprovalChain.ChainStatus.ARCHIVED);
        chainRepository.save(chain);
    }

    // ==================== Private Helper Methods ====================

    private void autoAdvanceToNextStep(ApprovalChainStep completedStep) {
        List<ApprovalChainStep> allSteps = stepRepository.findByApprovalIdOrderByStepLevelAsc(completedStep.getApprovalId());

        // Find the next pending step with a higher level
        for (ApprovalChainStep step : allSteps) {
            if (step.getStepLevel() > completedStep.getStepLevel()
                    && step.getStatus() == ApprovalChainStep.StepStatus.PENDING) {
                // The next step is already in PENDING status, it will be picked up by getCurrentStep
                log.info("Next step ready: approvalId={}, level={}", step.getApprovalId(), step.getStepLevel());
                break;
            }
        }
    }

    private void skipSubsequentSteps(ApprovalChainStep rejectedStep) {
        List<ApprovalChainStep> allSteps = stepRepository.findByApprovalIdOrderByStepLevelAsc(rejectedStep.getApprovalId());

        for (ApprovalChainStep step : allSteps) {
            if (step.getStepLevel() > rejectedStep.getStepLevel()
                    && step.getStatus() == ApprovalChainStep.StepStatus.PENDING) {
                step.setStatus(ApprovalChainStep.StepStatus.SKIPPED);
                step.setComment("前置步骤被拒绝，自动跳过");
                step.setActedAt(LocalDateTime.now());
                stepRepository.save(step);
            }
        }
    }
}
