package com.aiagent.repository;

import com.aiagent.entity.PasswordHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * Find password history for a user, ordered by most recent first.
     */
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find the N most recent password hashes for a user.
     */
    List<PasswordHistory> findTopNByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
