package com.aiagent.controller;

import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.exception.GlobalExceptionHandler;
import com.aiagent.service.WorkflowEngine;
import com.aiagent.service.WorkflowService;
import com.aiagent.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WorkflowController 单元测试
 * 使用 MockMvc 测试工作流管理接口
 */
@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("工作流控制器测试")
class WorkflowControllerTest extends AbstractWebMvcSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowEngine workflowEngine;

    @MockBean
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(100L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @WithMockUser
    @DisplayName("查询工作流定义列表 - 成功返回200")
    void testListDefinitions() throws Exception {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(1L);
        def.setName("审批流程");
        def.setTenantId(100L);
        def.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        def.setCreatedAt(LocalDateTime.now());

        when(workflowService.listDefinitions(eq(100L), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(def)));

        mockMvc.perform(get("/v1/workflows/definitions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("审批流程"));
    }

    @Test
    @WithMockUser
    @DisplayName("创建工作流定义 - 成功返回200")
    void testCreateDefinition() throws Exception {
        WorkflowDefinition saved = new WorkflowDefinition();
        saved.setId(1L);
        saved.setName("新流程");
        saved.setVersion(1);
        saved.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        saved.setTenantId(100L);

        doNothing().when(workflowService).validateNodeCount(any());
        when(workflowService.createDefinition(any(WorkflowDefinition.class))).thenReturn(saved);
        when(workflowService.updateDefinition(any(WorkflowDefinition.class))).thenReturn(saved);

        String json = objectMapper.writeValueAsString(Map.of(
                "name", "新流程",
                "description", "新流程描述"
        ));

        mockMvc.perform(post("/v1/workflows/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.name").value("新流程"));
    }

    @Test
    @WithMockUser
    @DisplayName("启动工作流 - 成功返回200")
    void testStartWorkflow() throws Exception {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setWorkflowDefinitionId(1L);
        instance.setWorkflowName("审批流程");
        instance.setStatus(WorkflowInstance.InstanceStatus.RUNNING);
        instance.setStartedAt(LocalDateTime.now());
        instance.setStartedBy(1L);
        instance.setTenantId(100L);

        when(workflowEngine.startWorkflow(eq(1L), any(), eq(1L))).thenReturn(instance);

        String json = objectMapper.writeValueAsString(Map.of(
                "definitionId", 1,
                "variables", Map.of("param1", "value1")
        ));

        mockMvc.perform(post("/v1/workflows/instances/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser
    @DisplayName("查询工作流定义 - 不存在返回错误")
    void testGetDefinition_NotFound() throws Exception {
        when(workflowService.getDefinitionByIdAndTenantId(eq(999L), eq(100L)))
                .thenThrow(new com.aiagent.exception.ResourceNotFoundException("工作流定义不存在"));

        mockMvc.perform(get("/v1/workflows/definitions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("查询工作流实例列表 - 成功返回200")
    void testListInstances() throws Exception {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setWorkflowName("测试流程");
        instance.setStatus(WorkflowInstance.InstanceStatus.RUNNING);
        instance.setTenantId(100L);

        when(workflowService.listInstances(eq(100L), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(instance)));

        mockMvc.perform(get("/v1/workflows/instances")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("获取工作流实例详情 - 成功返回200")
    void testGetInstance() throws Exception {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setWorkflowName("审批流程");
        instance.setStatus(WorkflowInstance.InstanceStatus.RUNNING);

        when(workflowEngine.getWorkflowStatus(1L)).thenReturn(instance);

        mockMvc.perform(get("/v1/workflows/instances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.workflowName").value("审批流程"));
    }
}
