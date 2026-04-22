package com.aiagent.service;

import com.aiagent.common.PageResult;
import com.aiagent.entity.DictItem;
import com.aiagent.entity.DictType;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.DictItemRepository;
import com.aiagent.repository.DictTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;

    // ==================== DictType CRUD ====================

    /**
     * 分页查询所有字典类型
     */
    public PageResult<DictType> getDictTypes(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException e) {
            direction = Sort.Direction.DESC;
        }
        Sort sort = Sort.by(direction, sortBy);
        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(page, size, sort);
        Page<DictType> result = dictTypeRepository.findAll(pageRequest);
        return PageResult.from(result);
    }

    /**
     * 根据ID获取字典类型
     */
    public DictType getDictType(Long id) {
        return dictTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "字典类型不存在, id=" + id));
    }

    /**
     * 创建字典类型
     */
    @Transactional
    @CacheEvict(value = "dictItemsMap", allEntries = true)
    public DictType createDictType(DictType dictType) {
        if (dictTypeRepository.existsByDictType(dictType.getDictType())) {
            throw new BusinessException(400, "字典类型编码已存在: " + dictType.getDictType());
        }
        if (dictType.getStatus() == null) {
            dictType.setStatus("active");
        }
        log.info("创建字典类型: {}", dictType.getDictType());
        return dictTypeRepository.save(dictType);
    }

    /**
     * 更新字典类型
     */
    @Transactional
    @CacheEvict(value = "dictItemsMap", allEntries = true)
    public DictType updateDictType(DictType dictType) {
        DictType existing = getDictType(dictType.getId());
        existing.setDictName(dictType.getDictName());
        existing.setRemark(dictType.getRemark());
        if (dictType.getStatus() != null) {
            existing.setStatus(dictType.getStatus());
        }
        if (dictType.getUpdatedBy() != null) {
            existing.setUpdatedBy(dictType.getUpdatedBy());
        }
        log.info("更新字典类型: id={}, dictType={}", dictType.getId(), existing.getDictType());
        return dictTypeRepository.save(existing);
    }

    /**
     * 删除字典类型（同时删除关联的字典项）
     */
    @Transactional
    @CacheEvict(value = "dictItemsMap", allEntries = true)
    public void deleteDictType(Long id) {
        DictType dictType = getDictType(id);
        dictItemRepository.deleteByDictType(dictType.getDictType());
        dictTypeRepository.deleteById(id);
        log.info("删除字典类型: id={}, dictType={}", id, dictType.getDictType());
    }

    // ==================== DictItem CRUD ====================

    /**
     * 根据字典类型编码获取字典项列表
     */
    public List<DictItem> getDictItems(String dictType) {
        return dictItemRepository.findByDictTypeAndStatusOrderByDictSort(dictType, "active");
    }

    /**
     * 批量获取字典项，返回Map结构供前端下拉选择使用
     */
    @Cacheable(value = "dictItemsMap", key = "#dictTypes.hashCode()")
    public Map<String, List<DictItem>> getDictItemsMap(List<String> dictTypes) {
        Map<String, List<DictItem>> result = new LinkedHashMap<>();
        if (dictTypes == null || dictTypes.isEmpty()) {
            return result;
        }
        List<DictItem> allItems = dictItemRepository.findByDictTypeInAndStatus(dictTypes, "active");
        for (DictItem item : allItems) {
            result.computeIfAbsent(item.getDictType(), k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * 创建字典项
     */
    @Transactional
    @CacheEvict(value = "dictItemsMap", allEntries = true)
    public DictItem createDictItem(DictItem dictItem) {
        // 验证字典类型是否存在
        if (!dictTypeRepository.existsByDictType(dictItem.getDictType())) {
            throw new BusinessException(400, "字典类型不存在: " + dictItem.getDictType());
        }
        if (dictItem.getDictSort() == null) {
            dictItem.setDictSort(0);
        }
        if (dictItem.getStatus() == null) {
            dictItem.setStatus("active");
        }
        if (dictItem.getIsDefault() == null) {
            dictItem.setIsDefault("N");
        }
        log.info("创建字典项: dictType={}, label={}", dictItem.getDictType(), dictItem.getDictLabel());
        return dictItemRepository.save(dictItem);
    }

    /**
     * 更新字典项
     */
    @Transactional
    @CacheEvict(value = "dictItemsMap", allEntries = true)
    public DictItem updateDictItem(DictItem dictItem) {
        DictItem existing = dictItemRepository.findById(dictItem.getId())
                .orElseThrow(() -> new BusinessException(404, "字典项不存在, id=" + dictItem.getId()));

        existing.setDictLabel(dictItem.getDictLabel());
        existing.setDictValue(dictItem.getDictValue());
        existing.setDictSort(dictItem.getDictSort());
        existing.setCssClass(dictItem.getCssClass());
        existing.setListClass(dictItem.getListClass());
        existing.setIsDefault(dictItem.getIsDefault());
        if (dictItem.getStatus() != null) {
            existing.setStatus(dictItem.getStatus());
        }
        existing.setRemark(dictItem.getRemark());

        log.info("更新字典项: id={}, dictType={}", dictItem.getId(), existing.getDictType());
        return dictItemRepository.save(existing);
    }

    /**
     * 删除字典项
     */
    @Transactional
    @CacheEvict(value = "dictItemsMap", allEntries = true)
    public void deleteDictItem(Long id) {
        DictItem existing = dictItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "字典项不存在, id=" + id));
        dictItemRepository.deleteById(id);
        log.info("删除字典项: id={}, dictType={}", id, existing.getDictType());
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据字典类型和值获取标签
     */
    public String getDictLabel(String dictType, String dictValue) {
        List<DictItem> items = dictItemRepository.findByDictTypeAndStatusOrderByDictSort(dictType, "active");
        return items.stream()
                .filter(item -> dictValue.equals(item.getDictValue()))
                .map(DictItem::getDictLabel)
                .findFirst()
                .orElse(dictValue);
    }

    /**
     * 获取所有活跃的字典类型列表（不分页）
     */
    public List<DictType> getAllActiveDictTypes() {
        return dictTypeRepository.findByStatusOrderByCreatedAtDesc("active");
    }
}
