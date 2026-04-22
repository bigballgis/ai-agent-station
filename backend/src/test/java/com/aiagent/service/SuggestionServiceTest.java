package com.aiagent.service;

import com.aiagent.entity.AgentEvolutionExperience;
import com.aiagent.entity.AgentEvolutionSuggestion;
import com.aiagent.repository.AgentEvolutionExperienceRepository;
import com.aiagent.repository.AgentEvolutionSuggestionRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SuggestionService 单元测试
 * 测试建议的创建、查询、状态更新、自动生成等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("建议服务测试")
class SuggestionServiceTest {

    @Mock
    private AgentEvolutionSuggestionRepository suggestionRepository;

    @Mock
    private AgentEvolutionExperienceRepository experienceRepository;

    @InjectMocks
    private SuggestionServiceImpl suggestionService;

    private AgentEvolutionSuggestion testSuggestion;
    private AgentEvolutionExperience testExperience;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;
    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @BeforeEach
    void setUp() {
        // 初始化测试建议数据
        testSuggestion = new AgentEvolutionSuggestion();
        testSuggestion.setId(1L);
        testSuggestion.setTenantId(100L);
        testSuggestion.setAgentId(1L);
        testSuggestion.setSuggestionType("process");
        testSuggestion.setTitle("流程优化建议");
        testSuggestion.setDescription("基于历史经验生成的优化建议");
        testSuggestion.setContent("{\"recommendations\": []}");
        testSuggestion.setPriority(1);
        testSuggestion.setStatus("PENDING");
        testSuggestion.setImplementationStatus("NOT_IMPLEMENTED");
        testSuggestion.setExpectedImpact(new BigDecimal("4.0"));
        testSuggestion.setCreatedBy(1L);
        testSuggestion.setCreatedAt(LocalDateTime.now());

        // 初始化测试经验数据
        testExperience = new AgentEvolutionExperience();
        testExperience.setId(1L);
        testExperience.setTenantId(100L);
        testExperience.setAgentId(1L);
        testExperience.setExperienceType("process");
        testExperience.setExperienceCode("EXP001");
        testExperience.setTitle("流程经验");
        testExperience.setContent("{\"step\": \"test\"}");
        testExperience.setStatus(1);
        testExperience.setCreatedBy(1L);
        testExperience.setCreatedAt(LocalDateTime.now());

        // 模拟租户上下文
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(100L);

        // 模拟安全上下文
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        UserPrincipal userPrincipal = new UserPrincipal(1L, "admin", "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList());
        org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(context);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
        securityContextHolderMock.close();
    }

    // ==================== createSuggestion 测试 ====================

    @Test
    @DisplayName("创建建议 - 成功")
    void createSuggestion_Success() {
        when(suggestionRepository.save(any(AgentEvolutionSuggestion.class)))
                .thenReturn(testSuggestion);

        AgentEvolutionSuggestion result = suggestionService.createSuggestion(testSuggestion);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("流程优化建议", result.getTitle());
        verify(suggestionRepository).save(testSuggestion);
    }

    @Test
    @DisplayName("创建建议 - 保存失败时抛出异常")
    void createSuggestion_SaveFailure_ThrowsException() {
        when(suggestionRepository.save(any(AgentEvolutionSuggestion.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        assertThrows(RuntimeException.class,
                () -> suggestionService.createSuggestion(testSuggestion));
    }

    // ==================== getSuggestions 测试 ====================

    @Test
    @DisplayName("获取所有建议 - 成功")
    void getAllSuggestions_Success() {
        List<AgentEvolutionSuggestion> suggestions = Arrays.asList(testSuggestion);
        when(suggestionRepository.findByTenantId(100L)).thenReturn(suggestions);

        List<AgentEvolutionSuggestion> result = suggestionService.getAllSuggestions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("流程优化建议", result.get(0).getTitle());
    }

    @Test
    @DisplayName("获取所有建议 - 空列表")
    void getAllSuggestions_Empty() {
        when(suggestionRepository.findByTenantId(100L)).thenReturn(Collections.emptyList());

        List<AgentEvolutionSuggestion> result = suggestionService.getAllSuggestions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("通过ID获取建议 - 成功")
    void getSuggestionById_Success() {
        when(suggestionRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testSuggestion));

        AgentEvolutionSuggestion result = suggestionService.getSuggestionById(1L);

        assertNotNull(result);
        assertEquals("流程优化建议", result.getTitle());
    }

    @Test
    @DisplayName("通过ID获取建议 - 不存在时返回null")
    void getSuggestionById_NotFound() {
        when(suggestionRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        AgentEvolutionSuggestion result = suggestionService.getSuggestionById(999L);

        assertNull(result);
    }

    // ==================== updateSuggestionStatus 测试 ====================

    @Test
    @DisplayName("更新建议状态 - 成功")
    void updateSuggestionStatus_Success() {
        AgentEvolutionSuggestion updateDetails = new AgentEvolutionSuggestion();
        updateDetails.setTitle("更新后的建议标题");
        updateDetails.setDescription("更新后的描述");

        when(suggestionRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testSuggestion));
        when(suggestionRepository.save(any(AgentEvolutionSuggestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentEvolutionSuggestion result = suggestionService.updateSuggestion(1L, updateDetails);

        assertNotNull(result);
        assertEquals("更新后的建议标题", result.getTitle());
        verify(suggestionRepository).save(any(AgentEvolutionSuggestion.class));
    }

    @Test
    @DisplayName("更新建议状态 - 建议不存在时返回null")
    void updateSuggestionStatus_NotFound() {
        AgentEvolutionSuggestion updateDetails = new AgentEvolutionSuggestion();
        updateDetails.setTitle("更新后的标题");

        when(suggestionRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        AgentEvolutionSuggestion result = suggestionService.updateSuggestion(999L, updateDetails);

        assertNull(result);
    }

    // ==================== generateSuggestions 测试 ====================

    @Test
    @DisplayName("生成建议 - 基于流程经验生成建议")
    void generateSuggestions_ProcessExperience() {
        List<AgentEvolutionExperience> experiences = Arrays.asList(testExperience);
        when(experienceRepository.findByAgentIdAndTenantId(1L, 100L))
                .thenReturn(experiences);
        when(suggestionRepository.save(any(AgentEvolutionSuggestion.class)))
                .thenAnswer(invocation -> {
                    AgentEvolutionSuggestion s = invocation.getArgument(0);
                    s.setId(10L);
                    return s;
                });

        List<AgentEvolutionSuggestion> result = suggestionService.generateSuggestions(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 应该基于流程经验生成流程优化建议
        assertTrue(result.stream().anyMatch(s -> "process".equals(s.getSuggestionType())));
        verify(suggestionRepository, atLeastOnce()).save(any(AgentEvolutionSuggestion.class));
    }

    @Test
    @DisplayName("生成建议 - 无经验数据时返回空列表")
    void generateSuggestions_NoExperiences() {
        when(experienceRepository.findByAgentIdAndTenantId(1L, 100L))
                .thenReturn(Collections.emptyList());

        List<AgentEvolutionSuggestion> result = suggestionService.generateSuggestions(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(suggestionRepository, never()).save(any(AgentEvolutionSuggestion.class));
    }

    @Test
    @DisplayName("生成建议 - 多种类型经验生成多种建议")
    void generateSuggestions_MultipleTypes() {
        AgentEvolutionExperience performanceExp = new AgentEvolutionExperience();
        performanceExp.setId(2L);
        performanceExp.setTenantId(100L);
        performanceExp.setAgentId(1L);
        performanceExp.setExperienceType("performance");
        performanceExp.setExperienceCode("EXP002");
        performanceExp.setTitle("性能经验");
        performanceExp.setContent("{\"metric\": \"response_time\"}");
        performanceExp.setStatus(1);
        performanceExp.setCreatedBy(1L);

        List<AgentEvolutionExperience> experiences = Arrays.asList(testExperience, performanceExp);
        when(experienceRepository.findByAgentIdAndTenantId(1L, 100L))
                .thenReturn(experiences);
        when(suggestionRepository.save(any(AgentEvolutionSuggestion.class)))
                .thenAnswer(invocation -> {
                    AgentEvolutionSuggestion s = invocation.getArgument(0);
                    s.setId(System.currentTimeMillis());
                    return s;
                });

        List<AgentEvolutionSuggestion> result = suggestionService.generateSuggestions(1L);

        assertNotNull(result);
        // 应该生成流程优化和性能优化两种建议
        assertTrue(result.size() >= 2);
    }

    // ==================== 删除建议测试 ====================

    @Test
    @DisplayName("删除建议 - 成功")
    void deleteSuggestion_Success() {
        when(suggestionRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testSuggestion));
        doNothing().when(suggestionRepository).delete(any(AgentEvolutionSuggestion.class));

        suggestionService.deleteSuggestion(1L);

        verify(suggestionRepository).delete(testSuggestion);
    }

    @Test
    @DisplayName("删除建议 - 不存在时不执行删除")
    void deleteSuggestion_NotFound() {
        when(suggestionRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        suggestionService.deleteSuggestion(999L);

        verify(suggestionRepository, never()).delete(any());
    }
}
