package com.aiagent.service;

import com.aiagent.config.properties.WorkflowProperties;
import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.repository.WorkflowInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowService 单元测试
 * 覆盖工作流定义的创建、更新、删除、发布、版本管理及节点/边数量校验
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工作流服务测试")
class WorkflowServiceTest {

    @Mock
    private WorkflowDefinitionRepository definitionRepository;

    @Mock
    private WorkflowInstanceRepository instanceRepository;

    @Mock
    private QuotaService quotaService;

    @Mock
    private WorkflowProperties workflowProperties;

    @InjectMocks
    private WorkflowService workflowService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(workflowProperties.getMaxNodeCount()).thenReturn(50);
        lenient().when(workflowProperties.getMaxEdgeCount()).thenReturn(100);
    }

    // ==================== 创建工作流定义 ====================

    @Nested
    @DisplayName("createDefinition 创建工作流定义")
    class CreateDefinitionTests {

        @Test
        @DisplayName("有效数据 - 创建成功并递增工作流计数")
        void createDefinition_withValidData_shouldSucceed() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setName("测试工作流");
            definition.setTenantId(TENANT_ID);
            definition.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);

            WorkflowDefinition saved = new WorkflowDefinition();
            saved.setId(10L);
            saved.setName("测试工作流");
            saved.setTenantId(TENANT_ID);
            saved.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);

            when(definitionRepository.save(definition)).thenReturn(saved);

            WorkflowDefinition result = workflowService.createDefinition(definition);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("测试工作流", result.getName());
            verify(quotaService).checkWorkflowQuota();
            verify(quotaService).incrementWorkflowCount();
            verify(definitionRepository).save(definition);
        }

        @Test
        @DisplayName("tenantId 为 null - 不执行配额检查和计数递增")
        void createDefinition_withNullTenantId_shouldSkipQuotaCheck() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setName("无租户工作流");
            definition.setTenantId(null);

            WorkflowDefinition saved = new WorkflowDefinition();
            saved.setId(11L);
            saved.setName("无租户工作流");

            when(definitionRepository.save(definition)).thenReturn(saved);

            WorkflowDefinition result = workflowService.createDefinition(definition);

            assertNotNull(result);
            verify(quotaService, never()).checkWorkflowQuota();
            verify(quotaService, never()).incrementWorkflowCount();
        }

        @Test
        @DisplayName("配额已满 - 抛出 BusinessException")
        void createDefinition_whenQuotaExceeded_shouldThrowException() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setName("超额工作流");
            definition.setTenantId(TENANT_ID);

            doThrow(new BusinessException("工作流数量已达上限")).when(quotaService).checkWorkflowQuota();

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> workflowService.createDefinition(definition));

            assertEquals("工作流数量已达上限", exception.getMessage());
            verify(definitionRepository, never()).save(any());
            verify(quotaService, never()).incrementWorkflowCount();
        }
    }

    // ==================== 更新工作流定义 ====================

    @Nested
    @DisplayName("updateDefinition 更新工作流定义")
    class UpdateDefinitionTests {

        @Test
        @DisplayName("更新工作流定义 - 成功保存")
        void updateDefinition_shouldSaveSuccessfully() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setId(1L);
            definition.setName("更新后工作流");
            definition.setTenantId(TENANT_ID);

            WorkflowDefinition saved = new WorkflowDefinition();
            saved.setId(1L);
            saved.setName("更新后工作流");

            when(definitionRepository.save(definition)).thenReturn(saved);

            WorkflowDefinition result = workflowService.updateDefinition(definition);

            assertNotNull(result);
            assertEquals("更新后工作流", result.getName());
            verify(definitionRepository).save(definition);
        }
    }

    // ==================== 删除工作流定义 ====================

    @Nested
    @DisplayName("deleteDefinition 删除工作流定义")
    class DeleteDefinitionTests {

        @Test
        @DisplayName("删除草稿定义 - 成功并递减计数")
        void deleteDefinition_draft_shouldSucceed() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setId(1L);
            definition.setTenantId(TENANT_ID);
            definition.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);

            workflowService.deleteDefinition(definition);

            verify(definitionRepository).delete(definition);
            verify(quotaService).decrementWorkflowCount();
        }

        @Test
        @DisplayName("删除已发布定义 - 仍然执行删除（当前实现无状态校验）")
        void deleteDefinition_published_shouldStillDelete() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setId(2L);
            definition.setTenantId(TENANT_ID);
            definition.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);

            workflowService.deleteDefinition(definition);

            verify(definitionRepository).delete(definition);
            verify(quotaService).decrementWorkflowCount();
        }

        @Test
        @DisplayName("tenantId 为 null - 不递减计数")
        void deleteDefinition_noTenant_shouldNotDecrementCount() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setId(3L);
            definition.setTenantId(null);

            workflowService.deleteDefinition(definition);

            verify(definitionRepository).delete(definition);
            verify(quotaService, never()).decrementWorkflowCount();
        }
    }

    // ==================== 发布工作流定义 ====================

    @Nested
    @DisplayName("publishDefinition 发布工作流定义")
    class PublishDefinitionTests {

        @Test
        @DisplayName("发布草稿定义 - 状态变为 PUBLISHED")
        void publishDefinition_shouldSetStatusToPublished() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setId(1L);
            definition.setName("待发布工作流");
            definition.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);

            WorkflowDefinition saved = new WorkflowDefinition();
            saved.setId(1L);
            saved.setName("待发布工作流");
            saved.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);

            when(definitionRepository.save(any(WorkflowDefinition.class))).thenReturn(saved);

            WorkflowDefinition result = workflowService.publishDefinition(definition);

            assertNotNull(result);
            assertEquals(WorkflowDefinition.WorkflowStatus.PUBLISHED, result.getStatus());
            verify(definitionRepository).save(definition);
        }
    }

    // ==================== 创建新版本 ====================

    @Nested
    @DisplayName("createNewVersion 创建新版本")
    class CreateNewVersionTests {

        @Test
        @DisplayName("基于已发布定义创建新版本 - 版本号递增，状态为 DRAFT")
        void createNewVersion_shouldIncrementVersionAndSetDraft() {
            WorkflowDefinition source = new WorkflowDefinition();
            source.setId(1L);
            source.setName("源工作流");
            source.setDescription("源描述");
            source.setVersion(2);
            source.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
            source.setBaseDefinitionId(null);
            source.setTenantId(TENANT_ID);

            when(definitionRepository.findByIdAndTenantId(1L, TENANT_ID))
                    .thenReturn(Optional.of(source));

            WorkflowDefinition savedNew = new WorkflowDefinition();
            savedNew.setId(5L);
            savedNew.setName("源工作流");
            savedNew.setVersion(3);
            savedNew.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
            savedNew.setBaseDefinitionId(1L);

            when(definitionRepository.save(any(WorkflowDefinition.class))).thenReturn(savedNew);

            WorkflowDefinition result = workflowService.createNewVersion(1L, TENANT_ID);

            assertNotNull(result);
            assertEquals(3, result.getVersion());
            assertEquals(WorkflowDefinition.WorkflowStatus.DRAFT, result.getStatus());
            assertEquals(1L, result.getBaseDefinitionId());
            verify(definitionRepository).save(argThat(d ->
                    d.getVersion() == 3 && d.getStatus() == WorkflowDefinition.WorkflowStatus.DRAFT
            ));
        }

        @Test
        @DisplayName("源定义不存在 - 抛出 ResourceNotFoundException")
        void createNewVersion_sourceNotFound_shouldThrowException() {
            when(definitionRepository.findByIdAndTenantId(99L, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> workflowService.createNewVersion(99L, TENANT_ID));
        }
    }

    // ==================== 回滚到指定版本 ====================

    @Nested
    @DisplayName("rollbackToVersion 回滚到指定版本")
    class RollbackToVersionTests {

        @Test
        @DisplayName("回滚到指定版本 - 创建新草稿并复制目标版本内容")
        void rollbackToVersion_shouldCreateNewDraftWithTargetContent() {
            WorkflowDefinition current = new WorkflowDefinition();
            current.setId(3L);
            current.setName("当前工作流");
            current.setDescription("当前描述");
            current.setVersion(3);
            current.setBaseDefinitionId(1L);
            current.setTenantId(TENANT_ID);

            WorkflowDefinition target = new WorkflowDefinition();
            target.setId(2L);
            target.setName("目标版本");
            target.setVersion(1);
            target.setBaseDefinitionId(1L);
            target.setTenantId(TENANT_ID);
            target.setNodes(Map.of("nodes", List.of("node1")));
            target.setEdges(Map.of("edges", List.of("edge1")));

            when(definitionRepository.findByIdAndTenantId(3L, TENANT_ID))
                    .thenReturn(Optional.of(current));
            when(definitionRepository.findByTenantId(TENANT_ID))
                    .thenReturn(List.of(current, target));

            WorkflowDefinition savedRollback = new WorkflowDefinition();
            savedRollback.setId(6L);
            savedRollback.setVersion(2);
            savedRollback.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
            savedRollback.setBaseDefinitionId(1L);

            when(definitionRepository.save(any(WorkflowDefinition.class))).thenReturn(savedRollback);

            WorkflowDefinition result = workflowService.rollbackToVersion(3L, 1L, TENANT_ID);

            assertNotNull(result);
            assertEquals(2, result.getVersion());
            assertEquals(WorkflowDefinition.WorkflowStatus.DRAFT, result.getStatus());
            verify(definitionRepository).save(argThat(d ->
                    d.getVersion() == 2
                    && d.getStatus() == WorkflowDefinition.WorkflowStatus.DRAFT
                    && d.getBaseDefinitionId().equals(1L)
            ));
        }

        @Test
        @DisplayName("目标版本不存在 - 抛出 BusinessException")
        void rollbackToVersion_targetNotFound_shouldThrowException() {
            WorkflowDefinition current = new WorkflowDefinition();
            current.setId(3L);
            current.setName("当前工作流");
            current.setVersion(3);
            current.setBaseDefinitionId(1L);
            current.setTenantId(TENANT_ID);

            when(definitionRepository.findByIdAndTenantId(3L, TENANT_ID))
                    .thenReturn(Optional.of(current));
            when(definitionRepository.findByTenantId(TENANT_ID))
                    .thenReturn(List.of(current));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> workflowService.rollbackToVersion(3L, 99L, TENANT_ID));

            assertTrue(exception.getMessage().contains("v99"));
        }
    }

    // ==================== 节点数量校验 ====================

    @Nested
    @DisplayName("validateNodeCount 节点数量校验")
    class ValidateNodeCountTests {

        @Test
        @DisplayName("节点数量未超限 - 不抛异常")
        void validateNodeCount_withinLimit_shouldNotThrow() {
            Map<String, Object> nodes = new HashMap<>();
            nodes.put("nodes", Collections.nCopies(50, "node"));

            assertDoesNotThrow(() -> workflowService.validateNodeCount(nodes));
        }

        @Test
        @DisplayName("节点数量超过限制 - 抛出 BusinessException")
        void validateNodeCount_exceedsLimit_shouldThrowException() {
            Map<String, Object> nodes = new HashMap<>();
            nodes.put("nodes", Collections.nCopies(51, "node"));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> workflowService.validateNodeCount(nodes));

            assertTrue(exception.getMessage().contains("51"));
            assertTrue(exception.getMessage().contains("50"));
        }

        @Test
        @DisplayName("nodes 为 null - 不抛异常")
        void validateNodeCount_nullNodes_shouldNotThrow() {
            assertDoesNotThrow(() -> workflowService.validateNodeCount(null));
        }

        @Test
        @DisplayName("nodes 中不含 nodes 键 - 不抛异常")
        void validateNodeCount_noNodesKey_shouldNotThrow() {
            Map<String, Object> nodes = new HashMap<>();
            nodes.put("other", "value");

            assertDoesNotThrow(() -> workflowService.validateNodeCount(nodes));
        }
    }

    // ==================== 边数量校验 ====================

    @Nested
    @DisplayName("validateEdgeCount 边数量校验")
    class ValidateEdgeCountTests {

        @Test
        @DisplayName("边数量未超限 - 不抛异常")
        void validateEdgeCount_withinLimit_shouldNotThrow() {
            Map<String, Object> edges = new HashMap<>();
            edges.put("edges", Collections.nCopies(100, "edge"));

            assertDoesNotThrow(() -> workflowService.validateEdgeCount(edges));
        }

        @Test
        @DisplayName("边数量超过限制 - 抛出 BusinessException")
        void validateEdgeCount_exceedsLimit_shouldThrowException() {
            Map<String, Object> edges = new HashMap<>();
            edges.put("edges", Collections.nCopies(101, "edge"));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> workflowService.validateEdgeCount(edges));

            assertTrue(exception.getMessage().contains("101"));
            assertTrue(exception.getMessage().contains("100"));
        }

        @Test
        @DisplayName("edges 为 null - 不抛异常")
        void validateEdgeCount_nullEdges_shouldNotThrow() {
            assertDoesNotThrow(() -> workflowService.validateEdgeCount(null));
        }
    }

    // ==================== 其他查询方法 ====================

    @Nested
    @DisplayName("getDefinitionByIdAndTenantId 查询工作流定义")
    class GetDefinitionTests {

        @Test
        @DisplayName("定义存在 - 返回定义")
        void getDefinitionByIdAndTenantId_found() {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setId(1L);
            definition.setName("存在的工作流");

            when(definitionRepository.findByIdAndTenantId(1L, TENANT_ID))
                    .thenReturn(Optional.of(definition));

            WorkflowDefinition result = workflowService.getDefinitionByIdAndTenantId(1L, TENANT_ID);

            assertNotNull(result);
            assertEquals("存在的工作流", result.getName());
        }

        @Test
        @DisplayName("定义不存在 - 抛出 ResourceNotFoundException")
        void getDefinitionByIdAndTenantId_notFound_shouldThrow() {
            when(definitionRepository.findByIdAndTenantId(99L, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> workflowService.getDefinitionByIdAndTenantId(99L, TENANT_ID));
        }
    }

    @Nested
    @DisplayName("existsByNameAndTenantId 名称存在性检查")
    class ExistsByNameTests {

        @Test
        @DisplayName("名称已存在 - 返回 true")
        void existsByNameAndTenantId_exists() {
            when(definitionRepository.existsByNameAndTenantId("已存在", TENANT_ID))
                    .thenReturn(true);

            assertTrue(workflowService.existsByNameAndTenantId("已存在", TENANT_ID));
        }

        @Test
        @DisplayName("名称不存在 - 返回 false")
        void existsByNameAndTenantId_notExists() {
            when(definitionRepository.existsByNameAndTenantId("不存在", TENANT_ID))
                    .thenReturn(false);

            assertFalse(workflowService.existsByNameAndTenantId("不存在", TENANT_ID));
        }
    }
}
