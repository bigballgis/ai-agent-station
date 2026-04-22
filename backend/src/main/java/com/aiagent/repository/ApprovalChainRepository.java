package com.aiagent.repository;

import com.aiagent.entity.ApprovalChain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalChainRepository extends JpaRepository<ApprovalChain, Long> {

    List<ApprovalChain> findByTenantId(Long tenantId);

    Page<ApprovalChain> findByTenantId(Long tenantId, Pageable pageable);

    Optional<ApprovalChain> findByIdAndTenantId(Long id, Long tenantId);

    List<ApprovalChain> findByTenantIdAndStatus(Long tenantId, ApprovalChain.ChainStatus status);
}
