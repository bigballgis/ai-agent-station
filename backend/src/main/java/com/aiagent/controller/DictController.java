package com.aiagent.controller;

import com.aiagent.annotation.RequiresRole;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.DictItem;
import com.aiagent.entity.DictType;
import com.aiagent.service.DictService;
import com.aiagent.vo.DictItemVO;
import com.aiagent.vo.DictTypeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据字典管理接口
 */
@RestController
@RequestMapping("/v1/dict-types")
@RequiredArgsConstructor
@Tag(name = "数据字典", description = "数据字典管理接口")
public class DictController {

    private final DictService dictService;

    // ==================== DictType CRUD ====================

    /**
     * 分页查询字典类型列表
     */
    @RequiresRole("ADMIN")
    @GetMapping
    public Result<PageResult<DictTypeVO>> getDictTypes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResult<DictType> result = dictService.getDictTypes(page, size, sortBy, sortDir);
        List<DictTypeVO> voList = result.getRecords().stream()
                .map(DictTypeVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(new PageResult<>(result.getTotal(), voList));
    }

    /**
     * 获取所有活跃字典类型（不分页）
     */
    @RequiresRole("ADMIN")
    @GetMapping("/active")
    @Operation(summary = "分页查询字典类型列表")
    public Result<List<DictTypeVO>> getActiveDictTypes() {
        List<DictTypeVO> voList = dictService.getAllActiveDictTypes().stream()
                .map(DictTypeVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 根据ID获取字典类型详情
     */
    @Operation(summary = "获取所有活跃字典类型")
    @RequiresRole("ADMIN")
    @GetMapping("/{id}")
    public Result<DictTypeVO> getDictType(@PathVariable Long id) {
        return Result.success(DictTypeVO.fromEntity(dictService.getDictType(id)));
    }

    /**
     * 创建字典类型
     */
    @Operation(summary = "根据ID获取字典类型详情")
    @RequiresRole("ADMIN")
    @PostMapping
    public Result<DictTypeVO> createDictType(@Valid @RequestBody DictType dictType) {
        return Result.success(DictTypeVO.fromEntity(dictService.createDictType(dictType)));
    }

    /**
     * 更新字典类型
     */
    @Operation(summary = "创建字典类型")
    @RequiresRole("ADMIN")
    @PutMapping("/{id}")
    public Result<DictTypeVO> updateDictType(@PathVariable Long id, @Valid @RequestBody DictType dictType) {
        dictType.setId(id);
        return Result.success(DictTypeVO.fromEntity(dictService.updateDictType(dictType)));
    }

    /**
     * 删除字典类型
     */
    @Operation(summary = "更新字典类型")
    @RequiresRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDictType(@PathVariable Long id) {
        dictService.deleteDictType(id);
        return Result.success();
    }

    // ==================== DictItem ====================

    /**
     * 根据字典类型编码获取字典项列表
     */
    @Operation(summary = "删除字典类型")
    @RequiresRole("ADMIN")
    @GetMapping("/{dictType}/items")
    public Result<List<DictItemVO>> getDictItems(@PathVariable String dictType) {
        List<DictItemVO> voList = dictService.getDictItems(dictType).stream()
                .map(DictItemVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 批量获取字典项（Map结构，供前端下拉选择使用）
     */
    @Operation(summary = "根据字典类型编码获取字典项列表")
    @RequiresRole("ADMIN")
    @GetMapping("/dict-items/batch")
    public Result<Map<String, List<DictItemVO>>> getDictItemsMap(@RequestParam List<String> dictTypes) {
        Map<String, List<DictItem>> itemMap = dictService.getDictItemsMap(dictTypes);
        Map<String, List<DictItemVO>> voMap = itemMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().map(DictItemVO::fromEntity).collect(Collectors.toList())
                ));
        return Result.success(voMap);
    }

    /**
     * 创建字典项
     */
    @Operation(summary = "批量获取字典项")
    @RequiresRole("ADMIN")
    @PostMapping("/dict-items")
    public Result<DictItemVO> createDictItem(@Valid @RequestBody DictItem dictItem) {
        return Result.success(DictItemVO.fromEntity(dictService.createDictItem(dictItem)));
    }

    /**
     * 更新字典项
     */
    @Operation(summary = "创建字典项")
    @RequiresRole("ADMIN")
    @PutMapping("/dict-items/{id}")
    public Result<DictItemVO> updateDictItem(@PathVariable Long id, @Valid @RequestBody DictItem dictItem) {
        dictItem.setId(id);
        return Result.success(DictItemVO.fromEntity(dictService.updateDictItem(dictItem)));
    }

    /**
     * 删除字典项
     */
    @Operation(summary = "更新字典项")
    @RequiresRole("ADMIN")
    @DeleteMapping("/dict-items/{id}")
    public Result<Void> deleteDictItem(@PathVariable Long id) {
        dictService.deleteDictItem(id);
        return Result.success();
    }
}
