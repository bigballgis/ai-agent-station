package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.entity.DictItem;
import com.aiagent.entity.DictType;
import com.aiagent.service.DictService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DictController 单元测试
 * 使用 MockMvc 测试字典管理接口
 */
@WebMvcTest(DictController.class)
@DisplayName("字典控制器测试")
class DictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DictService dictService;

    @Test
    @DisplayName("分页查询字典类型 - 成功返回200")
    void testGetDictTypes() throws Exception {
        DictType dictType = new DictType();
        dictType.setId(1L);
        dictType.setDictName("用户状态");
        dictType.setDictType("user_status");

        PageResult<DictType> pageResult = PageResult.of(1L, List.of(dictType));
        when(dictService.getDictTypes(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/dict-types")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].dictName").value("用户状态"));
    }

    @Test
    @DisplayName("创建字典类型 - 成功返回200")
    void testCreateDictType() throws Exception {
        DictType newType = new DictType();
        newType.setId(2L);
        newType.setDictName("订单状态");
        newType.setDictType("order_status");
        newType.setStatus("active");

        when(dictService.createDictType(any(DictType.class))).thenReturn(newType);

        String json = objectMapper.writeValueAsString(Map.of(
                "dictName", "订单状态",
                "dictType", "order_status"
        ));

        mockMvc.perform(post("/dict-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dictType").value("order_status"));
    }

    @Test
    @DisplayName("根据类型获取字典项 - 成功返回200")
    void testGetDictItems() throws Exception {
        DictItem item = new DictItem();
        item.setId(1L);
        item.setDictType("user_status");
        item.setDictLabel("启用");
        item.setDictValue("1");

        when(dictService.getDictItems("user_status")).thenReturn(List.of(item));

        mockMvc.perform(get("/dict-types/user_status/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].dictLabel").value("启用"))
                .andExpect(jsonPath("$.data[0].dictValue").value("1"));
    }

    @Test
    @DisplayName("根据ID获取字典类型 - 成功返回200")
    void testGetDictTypeById() throws Exception {
        DictType dictType = new DictType();
        dictType.setId(1L);
        dictType.setDictName("用户状态");
        dictType.setDictType("user_status");

        when(dictService.getDictType(1L)).thenReturn(dictType);

        mockMvc.perform(get("/dict-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("删除字典类型 - 成功返回200")
    void testDeleteDictType() throws Exception {
        doNothing().when(dictService).deleteDictType(1L);

        mockMvc.perform(delete("/dict-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dictService).deleteDictType(1L);
    }

    @Test
    @DisplayName("批量获取字典项Map - 成功返回200")
    void testGetDictItemsMap() throws Exception {
        when(dictService.getDictItemsMap(anyList())).thenReturn(new LinkedHashMap<>());

        mockMvc.perform(get("/dict-types/dict-items/batch")
                        .param("dictTypes", "user_status,order_status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
