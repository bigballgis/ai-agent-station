package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.DictItem;
import com.aiagent.entity.DictType;
import com.aiagent.service.DictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据字典管理接口
 */
@RestController
@RequestMapping("/dict-types")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ==================== DictType CRUD ====================

    /**
     * 分页查询字典类型列表
     */
    @GetMapping
    public Result<PageResult<DictType>> getDictTypes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return Result.success(dictService.getDictTypes(page, size, sortBy, sortDir));
    }

    /**
     * 获取所有活跃字典类型（不分页）
     */
    @GetMapping("/active")
    public Result<List<DictType>> getActiveDictTypes() {
        return Result.success(dictService.getAllActiveDictTypes());
    }

    /**
     * 根据ID获取字典类型详情
     */
    @GetMapping("/{id}")
    public Result<DictType> getDictType(@PathVariable Long id) {
        return Result.success(dictService.getDictType(id));
    }

    /**
     * 创建字典类型
     */
    @PostMapping
    public Result<DictType> createDictType(@Valid @RequestBody DictType dictType) {
        return Result.success(dictService.createDictType(dictType));
    }

    /**
     * 更新字典类型
     */
    @PutMapping("/{id}")
    public Result<DictType> updateDictType(@PathVariable Long id, @Valid @RequestBody DictType dictType) {
        dictType.setId(id);
        return Result.success(dictService.updateDictType(dictType));
    }

    /**
     * 删除字典类型
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDictType(@PathVariable Long id) {
        dictService.deleteDictType(id);
        return Result.success();
    }

    // ==================== DictItem ====================

    /**
     * 根据字典类型编码获取字典项列表
     */
    @GetMapping("/{dictType}/items")
    public Result<List<DictItem>> getDictItems(@PathVariable String dictType) {
        return Result.success(dictService.getDictItems(dictType));
    }

    /**
     * 批量获取字典项（Map结构，供前端下拉选择使用）
     */
    @GetMapping("/dict-items/batch")
    public Result<Map<String, List<DictItem>>> getDictItemsMap(@RequestParam List<String> dictTypes) {
        return Result.success(dictService.getDictItemsMap(dictTypes));
    }

    /**
     * 创建字典项
     */
    @PostMapping("/dict-items")
    public Result<DictItem> createDictItem(@Valid @RequestBody DictItem dictItem) {
        return Result.success(dictService.createDictItem(dictItem));
    }

    /**
     * 更新字典项
     */
    @PutMapping("/dict-items/{id}")
    public Result<DictItem> updateDictItem(@PathVariable Long id, @Valid @RequestBody DictItem dictItem) {
        dictItem.setId(id);
        return Result.success(dictService.updateDictItem(dictItem));
    }

    /**
     * 删除字典项
     */
    @DeleteMapping("/dict-items/{id}")
    public Result<Void> deleteDictItem(@PathVariable Long id) {
        dictService.deleteDictItem(id);
        return Result.success();
    }
}
