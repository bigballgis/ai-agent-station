package com.aiagent.repository;

import com.aiagent.entity.ApprovalChainStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface ApprovalChainStepRepository extends JpaRepository<ApprovalChainStep, Long> {

    List<ApprovalChainStep> findByChainIdOrderByStepLevelAsc(Long chainId);

    List<ApprovalChainStep> findByApprovalIdOrderByStepLevelAsc(Long approvalId);

    Optional<ApprovalChainStep> findByApprovalIdAndStepLevel(Long approvalId, Integer stepLevel);

    List<ApprovalChainStep> findByApprovalIdAndStatus(Long approvalId, ApprovalChainStep.StepStatus status);

    List<ApprovalChainStep> findByChainIdAndApprovalId(Long chainId, Long approvalId);
}
