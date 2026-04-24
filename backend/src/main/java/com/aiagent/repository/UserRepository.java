package com.aiagent.repository;

import com.aiagent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByTenantId(Long tenantId);
    Optional<User> findByUsernameAndTenantId(String username, Long tenantId);

    /**
     * Find users whose lockout period has expired (locked_until < now)
     */
    List<User> findByLockedUntilBeforeAndLockedUntilIsNotNull(LocalDateTime now);
}
