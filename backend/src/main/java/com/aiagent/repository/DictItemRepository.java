package com.aiagent.repository;

import com.aiagent.entity.DictItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictItemRepository extends JpaRepository<DictItem, Long> {
    List<DictItem> findByDictTypeAndStatusOrderByDictSort(String dictType, String status);

    List<DictItem> findByDictTypeOrderByDictSort(String dictType);

    List<DictItem> findByDictTypeInAndStatus(List<String> dictTypes, String status);

    void deleteByDictType(String dictType);
}
