package com.aiagent.repository;

import com.aiagent.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"tenant"})
    List<User> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"tenant"})
    Page<User> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<User> findByUsernameAndTenantId(String username, Long tenantId);

    /**
     * Find users whose lockout period has expired (locked_until < now)
     */
    List<User> findByLockedUntilBeforeAndLockedUntilIsNotNull(LocalDateTime now);

    /**
     * 统计租户下活跃用户数量
     */
    long countByTenantIdAndIsActiveTrue(Long tenantId);

    /**
     * 查询租户下指定时间范围内创建的用户
     */
    @EntityGraph(attributePaths = {"tenant"})
    List<User> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end);

    /**
     * 查询租户下指定时间范围内创建的用户（分页）
     */
    @EntityGraph(attributePaths = {"tenant"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<User> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 检查租户下是否存在指定用户名
     */
    boolean existsByUsernameAndTenantId(String username, Long tenantId);

    /**
     * 检查租户下是否存在指定邮箱
     */
    boolean existsByEmailAndTenantId(String email, Long tenantId);

    /**
     * 按邮箱查询用户（租户隔离）
     */
    @EntityGraph(attributePaths = {"tenant"})
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    /**
     * 软删除租户下所有用户（租户清理）
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.deleted = true WHERE u.tenantId = :tenantId")
    int softDeleteAllByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 统计租户下用户总数
     */
    long countByTenantId(Long tenantId);

    /**
     * 查询租户下指定邮箱前缀的用户列表
     */
    @EntityGraph(attributePaths = {"tenant"})
    List<User> findByTenantIdAndEmailStartingWith(Long tenantId, String emailPrefix);
}
