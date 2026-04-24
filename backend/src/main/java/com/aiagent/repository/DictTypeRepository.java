package com.aiagent.repository;

import com.aiagent.entity.DictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface DictTypeRepository extends JpaRepository<DictType, Long>, JpaSpecificationExecutor<DictType> {
    Optional<DictType> findByDictType(String dictType);

    List<DictType> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByDictType(String dictType);
}
