package com.aiagent.repository;

import com.aiagent.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
    List<UserRole> findByRoleId(Long roleId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.userId IN :userIds")
    List<UserRole> findByUserIdIn(@Param("userIds") List<Long> userIds);

    @Transactional
    @Modifying
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
