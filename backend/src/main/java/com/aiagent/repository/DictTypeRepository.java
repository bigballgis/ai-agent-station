package com.aiagent.repository;

import com.aiagent.entity.DictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DictTypeRepository extends JpaRepository<DictType, Long>, JpaSpecificationExecutor<DictType> {
    Optional<DictType> findByDictType(String dictType);

    List<DictType> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByDictType(String dictType);
}
