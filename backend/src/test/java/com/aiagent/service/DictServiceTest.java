package com.aiagent.service;

import com.aiagent.common.PageResult;
import com.aiagent.entity.DictItem;
import com.aiagent.entity.DictType;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.DictItemRepository;
import com.aiagent.repository.DictTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DictService 单元测试
 * 测试字典类型和字典项的增删改查功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("字典服务测试")
class DictServiceTest {

    @Mock
    private DictTypeRepository dictTypeRepository;

    @Mock
    private DictItemRepository dictItemRepository;

    @InjectMocks
    private DictService dictService;

    private DictType testDictType;
    private DictItem testDictItem;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testDictType = new DictType();
        testDictType.setId(1L);
        testDictType.setDictName("用户状态");
        testDictType.setDictType("user_status");
        testDictType.setStatus("active");
        testDictType.setRemark("用户状态字典");

        testDictItem = new DictItem();
        testDictItem.setId(1L);
        testDictItem.setDictType("user_status");
        testDictItem.setDictLabel("启用");
        testDictItem.setDictValue("1");
        testDictItem.setDictSort(1);
        testDictItem.setStatus("active");
    }

    // ==================== 字典类型测试 ====================

    @Test
    @DisplayName("分页查询字典类型 - 成功")
    void testGetDictTypes() {
        // 准备测试数据
        List<DictType> dictTypes = List.of(testDictType);
        Page<DictType> page = new PageImpl<>(dictTypes);
        when(dictTypeRepository.findAll(any(Pageable.class))).thenReturn(page);

        // 执行测试
        PageResult<DictType> result = dictService.getDictTypes(0, 10, "createdAt", "desc");

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(dictTypeRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询字典类型 - 无效排序方向使用默认降序")
    void testGetDictTypes_InvalidSortDir() {
        List<DictType> dictTypes = List.of(testDictType);
        Page<DictType> page = new PageImpl<>(dictTypes);
        when(dictTypeRepository.findAll(any(Pageable.class))).thenReturn(page);

        // 使用无效的排序方向，应默认使用 DESC
        PageResult<DictType> result = dictService.getDictTypes(0, 10, "createdAt", "invalid");

        assertNotNull(result);
        verify(dictTypeRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("创建字典类型 - 成功")
    void testCreateDictType() {
        DictType newType = new DictType();
        newType.setDictName("订单状态");
        newType.setDictType("order_status");

        when(dictTypeRepository.existsByDictType("order_status")).thenReturn(false);
        when(dictTypeRepository.save(any(DictType.class))).thenAnswer(invocation -> {
            DictType saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setStatus("active");
            return saved;
        });

        DictType result = dictService.createDictType(newType);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("active", result.getStatus());
        verify(dictTypeRepository).save(any(DictType.class));
    }

    @Test
    @DisplayName("创建字典类型 - 编码已存在抛出异常")
    void testCreateDictType_DuplicateCode() {
        when(dictTypeRepository.existsByDictType("user_status")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            dictService.createDictType(testDictType);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("字典类型编码已存在"));
        verify(dictTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新字典类型 - 成功")
    void testUpdateDictType() {
        when(dictTypeRepository.findById(1L)).thenReturn(Optional.of(testDictType));
        when(dictTypeRepository.save(any(DictType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DictType updateData = new DictType();
        updateData.setId(1L);
        updateData.setDictName("用户状态（已更新）");
        updateData.setRemark("更新后的备注");
        updateData.setUpdatedBy("admin");

        DictType result = dictService.updateDictType(updateData);

        assertNotNull(result);
        assertEquals("用户状态（已更新）", result.getDictName());
        assertEquals("更新后的备注", result.getRemark());
        assertEquals("admin", result.getUpdatedBy());
        verify(dictTypeRepository).save(any(DictType.class));
    }

    @Test
    @DisplayName("更新字典类型 - 不存在抛出异常")
    void testUpdateDictType_NotFound() {
        when(dictTypeRepository.findById(999L)).thenReturn(Optional.empty());

        DictType updateData = new DictType();
        updateData.setId(999L);

        assertThrows(BusinessException.class, () -> dictService.updateDictType(updateData));
    }

    @Test
    @DisplayName("删除字典类型 - 成功")
    void testDeleteDictType() {
        when(dictTypeRepository.findById(1L)).thenReturn(Optional.of(testDictType));
        doNothing().when(dictItemRepository).deleteByDictType("user_status");
        doNothing().when(dictTypeRepository).deleteById(1L);

        dictService.deleteDictType(1L);

        verify(dictItemRepository).deleteByDictType("user_status");
        verify(dictTypeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("删除字典类型 - 不存在抛出异常")
    void testDeleteDictType_NotFound() {
        when(dictTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> dictService.deleteDictType(999L));
    }

    // ==================== 字典项测试 ====================

    @Test
    @DisplayName("根据类型获取字典项列表 - 成功")
    void testGetDictItems() {
        List<DictItem> items = List.of(testDictItem);
        when(dictItemRepository.findByDictTypeAndStatusOrderByDictSort("user_status", "active"))
                .thenReturn(items);

        List<DictItem> result = dictService.getDictItems("user_status");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("启用", result.get(0).getDictLabel());
    }

    @Test
    @DisplayName("批量获取字典项Map - 成功")
    void testGetDictItemsMap() {
        DictItem item2 = new DictItem();
        item2.setId(2L);
        item2.setDictType("order_status");
        item2.setDictLabel("待支付");
        item2.setDictValue("0");
        item2.setStatus("active");

        List<DictItem> allItems = Arrays.asList(testDictItem, item2);
        when(dictItemRepository.findByDictTypeInAndStatus(
                eq(List.of("user_status", "order_status")), eq("active")))
                .thenReturn(allItems);

        Map<String, List<DictItem>> result = dictService.getDictItemsMap(
                List.of("user_status", "order_status"));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("user_status"));
        assertTrue(result.containsKey("order_status"));
    }

    @Test
    @DisplayName("批量获取字典项Map - 空列表返回空Map")
    void testGetDictItemsMap_EmptyList() {
        Map<String, List<DictItem>> result = dictService.getDictItemsMap(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("批量获取字典项Map - null列表返回空Map")
    void testGetDictItemsMap_NullList() {
        Map<String, List<DictItem>> result = dictService.getDictItemsMap(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("创建字典项 - 成功")
    void testCreateDictItem() {
        when(dictTypeRepository.existsByDictType("user_status")).thenReturn(true);
        when(dictItemRepository.save(any(DictItem.class))).thenAnswer(invocation -> {
            DictItem item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });

        DictItem newItem = new DictItem();
        newItem.setDictType("user_status");
        newItem.setDictLabel("禁用");
        newItem.setDictValue("0");

        DictItem result = dictService.createDictItem(newItem);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("active", result.getStatus());
        assertEquals(0, result.getDictSort());
        assertEquals("N", result.getIsDefault());
    }

    @Test
    @DisplayName("创建字典项 - 字典类型不存在抛出异常")
    void testCreateDictItem_DictTypeNotFound() {
        when(dictTypeRepository.existsByDictType("nonexistent")).thenReturn(false);

        DictItem newItem = new DictItem();
        newItem.setDictType("nonexistent");
        newItem.setDictLabel("测试");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            dictService.createDictItem(newItem);
        });

        assertEquals(400, exception.getCode());
    }
}
