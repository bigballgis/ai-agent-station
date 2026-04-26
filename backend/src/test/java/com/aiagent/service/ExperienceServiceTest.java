package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.AgentEvolutionExperience;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentEvolutionExperienceRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExperienceService 单元测试
 * 测试经验的增删改查、搜索、去重等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("经验服务测试")
class ExperienceServiceTest {

    @Mock
    private AgentEvolutionExperienceRepository experienceRepository;

    @InjectMocks
    private ExperienceServiceImpl experienceService;

    private AgentEvolutionExperience testExperience;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;
    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @BeforeEach
    void setUp() {
        // 初始化测试经验数据
        testExperience = new AgentEvolutionExperience();
        testExperience.setId(1L);
        testExperience.setTenantId(100L);
        testExperience.setAgentId(1L);
        testExperience.setExperienceType("process");
        testExperience.setExperienceCode("EXP001");
        testExperience.setTitle("测试经验");
        testExperience.setDescription("测试描述");
        testExperience.setContent("{\"key\": \"value\"}");
        testExperience.setTags(Arrays.asList("tag1", "tag2"));
        testExperience.setUsageCount(0);
        testExperience.setEffectivenessScore(new BigDecimal("4.5"));
        testExperience.setStatus(1);
        testExperience.setCreatedBy(1L);
        testExperience.setCreatedAt(LocalDateTime.now());

        // 模拟租户上下文
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(100L);

        // 模拟安全上下文
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        UserPrincipal userPrincipal = new UserPrincipal(1L, "admin", 100L);
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

    // ==================== createExperience 测试 ====================

    @Test
    @DisplayName("创建经验 - 成功")
    void createExperience_Success() {
        // 模拟经验代码不存在
        when(experienceRepository.findByExperienceCodeAndTenantId("EXP001", 100L))
                .thenReturn(Optional.empty());
        when(experienceRepository.save(any(AgentEvolutionExperience.class)))
                .thenReturn(testExperience);

        AgentEvolutionExperience result = experienceService.createExperience(testExperience);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试经验", result.getTitle());
        assertEquals(0, result.getUsageCount());
        assertEquals(1, result.getStatus());
        verify(experienceRepository).save(any(AgentEvolutionExperience.class));
    }

    @Test
    @DisplayName("创建经验 - 经验代码已存在时抛出异常")
    void createExperience_DuplicateCode_ThrowsException() {
        when(experienceRepository.findByExperienceCodeAndTenantId("EXP001", 100L))
                .thenReturn(Optional.of(testExperience));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> experienceService.createExperience(testExperience));

        assertEquals(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(experienceRepository, never()).save(any());
    }

    // ==================== getExperiences 测试 ====================

    @Test
    @DisplayName("获取所有经验 - 成功")
    void getAllExperiences_Success() {
        List<AgentEvolutionExperience> experiences = Arrays.asList(testExperience);
        when(experienceRepository.findByTenantId(100L)).thenReturn(experiences);

        List<AgentEvolutionExperience> result = experienceService.getAllExperiences();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试经验", result.get(0).getTitle());
    }

    @Test
    @DisplayName("获取所有经验 - 空列表")
    void getAllExperiences_Empty() {
        when(experienceRepository.findByTenantId(100L)).thenReturn(Collections.emptyList());

        List<AgentEvolutionExperience> result = experienceService.getAllExperiences();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== updateExperience 测试 ====================

    @Test
    @DisplayName("更新经验 - 成功")
    void updateExperience_Success() {
        AgentEvolutionExperience updateDetails = new AgentEvolutionExperience();
        updateDetails.setTitle("更新后的标题");
        updateDetails.setDescription("更新后的描述");

        when(experienceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testExperience));
        when(experienceRepository.save(any(AgentEvolutionExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentEvolutionExperience result = experienceService.updateExperience(1L, updateDetails);

        assertNotNull(result);
        assertEquals("更新后的标题", result.getTitle());
        assertEquals("更新后的描述", result.getDescription());
        verify(experienceRepository).save(any(AgentEvolutionExperience.class));
    }

    @Test
    @DisplayName("更新经验 - 经验不存在时抛出异常")
    void updateExperience_NotFound_ThrowsException() {
        AgentEvolutionExperience updateDetails = new AgentEvolutionExperience();
        updateDetails.setTitle("更新后的标题");

        when(experienceRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> experienceService.updateExperience(999L, updateDetails));
    }

    // ==================== deleteExperience 测试 ====================

    @Test
    @DisplayName("删除经验 - 成功")
    void deleteExperience_Success() {
        when(experienceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testExperience));
        doNothing().when(experienceRepository).delete(any(AgentEvolutionExperience.class));

        experienceService.deleteExperience(1L);

        verify(experienceRepository).delete(testExperience);
    }

    @Test
    @DisplayName("删除经验 - 经验不存在时抛出异常")
    void deleteExperience_NotFound_ThrowsException() {
        when(experienceRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> experienceService.deleteExperience(999L));
    }

    // ==================== searchExperiences 测试 ====================

    @Test
    @DisplayName("搜索经验 - 按关键词搜索")
    void searchExperiences_ByKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AgentEvolutionExperience> page = new PageImpl<>(Arrays.asList(testExperience));

        when(experienceRepository.findAll(ArgumentMatchers.<Specification<AgentEvolutionExperience>>any(), eq(pageable))).thenReturn(page);

        Page<AgentEvolutionExperience> result = experienceService.searchExperiences(
                "测试", null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(experienceRepository).findAll(ArgumentMatchers.<Specification<AgentEvolutionExperience>>any(), eq(pageable));
    }

    @Test
    @DisplayName("搜索经验 - 按类型和标签搜索")
    void searchExperiences_ByTypeAndTags() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AgentEvolutionExperience> page = new PageImpl<>(Arrays.asList(testExperience));

        when(experienceRepository.findAll(ArgumentMatchers.<Specification<AgentEvolutionExperience>>any(), eq(pageable))).thenReturn(page);

        Page<AgentEvolutionExperience> result = experienceService.searchExperiences(
                null, "process", Arrays.asList("tag1"), pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("搜索经验 - 无条件搜索返回全部")
    void searchExperiences_NoConditions() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AgentEvolutionExperience> page = new PageImpl<>(Arrays.asList(testExperience));

        when(experienceRepository.findAll(ArgumentMatchers.<Specification<AgentEvolutionExperience>>any(), eq(pageable))).thenReturn(page);

        Page<AgentEvolutionExperience> result = experienceService.searchExperiences(
                null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== 其他功能测试 ====================

    @Test
    @DisplayName("增加使用次数 - 成功")
    void incrementUsageCount_Success() {
        when(experienceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testExperience));
        when(experienceRepository.save(any(AgentEvolutionExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        experienceService.incrementUsageCount(1L);

        assertEquals(1, testExperience.getUsageCount());
        verify(experienceRepository).save(testExperience);
    }

    @Test
    @DisplayName("获取经验 - 通过Agent ID查询")
    void getExperiencesByAgentId_Success() {
        when(experienceRepository.findByAgentIdAndTenantId(1L, 100L))
                .thenReturn(Arrays.asList(testExperience));

        List<AgentEvolutionExperience> result = experienceService.getExperiencesByAgentId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
