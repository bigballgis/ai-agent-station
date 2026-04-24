package com.aiagent.repository;

import com.aiagent.entity.DictItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface DictItemRepository extends JpaRepository<DictItem, Long> {
    List<DictItem> findByDictTypeAndStatusOrderByDictSort(String dictType, String status);

    List<DictItem> findByDictTypeOrderByDictSort(String dictType);

    List<DictItem> findByDictTypeInAndStatus(List<String> dictTypes, String status);

    @Transactional
    @Modifying
    void deleteByDictType(String dictType);
}
