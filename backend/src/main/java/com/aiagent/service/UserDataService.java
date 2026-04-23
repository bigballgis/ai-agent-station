package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentTestCase;
import com.aiagent.entity.AgentTestExecution;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.entity.LoginLog;
import com.aiagent.entity.SystemLog;
import com.aiagent.entity.User;
import com.aiagent.entity.UserSession;
import com.aiagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * GDPR/隐私合规服务
 * 提供用户数据导出、删除、匿名化和数据保留报告功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataService {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final AgentTestResultRepository testResultRepository;
    private final AgentTestExecutionRepository testExecutionRepository;
    private final AgentTestCaseRepository testCaseRepository;
    private final SystemLogRepository systemLogRepository;
    private final LoginLogRepository loginLogRepository;
    private final UserSessionRepository userSessionRepository;
    private final DeploymentHistoryRepository deploymentHistoryRepository;
    private final AgentApprovalRepository agentApprovalRepository;

    /**
     * 导出用户的所有数据
     *
     * @param userId 用户ID
     * @return 包含用户所有数据的Map
     */
    public Map<String, Object> exportUserData(Long userId) {
        log.info("导出用户数据: userId={}", userId);

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("exportTime", LocalDateTime.now().toString());
        userData.put("userId", userId);

        // 用户基本信息
        userRepository.findById(userId).ifPresent(user -> {
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
            userInfo.put("tenantId", user.getTenantId());
            userInfo.put("isActive", user.getIsActive());
            userInfo.put("createdAt", user.getCreatedAt());
            userInfo.put("updatedAt", user.getUpdatedAt());
            userData.put("userInfo", userInfo);
        });

        // 用户创建的Agent (通过tenantId查询后过滤)
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getTenantId() != null) {
                userData.put("agents", agentRepository.findByTenantId(user.getTenantId()).stream()
                        .filter(a -> userId.equals(a.getCreatedBy()))
                        .map(this::agentToMap).toList());
            }
        });

        // 测试用例 (通过tenantId查询后过滤)
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getTenantId() != null) {
                userData.put("testCases", testCaseRepository.findByTenantId(user.getTenantId()).stream()
                        .filter(tc -> userId.equals(tc.getCreatedBy()))
                        .map(this::testCaseToMap).toList());
            }
        });

        // 测试执行记录 (通过tenantId查询)
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getTenantId() != null) {
                userData.put("testExecutions", testExecutionRepository.findByTenantId(user.getTenantId()).stream()
                        .map(this::executionToMap).toList());
            }
        });

        // 测试结果 (通过tenantId查询)
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getTenantId() != null) {
                userData.put("testResults", testResultRepository.findByTenantId(user.getTenantId()).stream()
                        .map(this::testResultToMap).toList());
            }
        });

        // 系统日志 (通过tenantId查询)
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getTenantId() != null) {
                userData.put("systemLogs", systemLogRepository.findByTenantId(user.getTenantId()).stream()
                        .filter(l -> userId.equals(l.getUserId()))
                        .map(this::systemLogToMap).toList());
            }
        });

        // 登录日志
        userData.put("loginLogs", loginLogRepository.findByUserIdOrderByLoginTimeDesc(userId).stream()
                .map(this::loginLogToMap).toList());

        // 会话记录
        userData.put("sessions", userSessionRepository.findByUserId(userId).stream()
                .map(this::sessionToMap).toList());

        return userData;
    }

    /**
     * 删除用户的所有数据（匿名化处理，非硬删除）
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserData(Long userId) {
        log.info("删除用户数据(匿名化): userId={}", userId);
        anonymizeUser(userId);
    }

    /**
     * 匿名化用户数据，替换所有PII为匿名值
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void anonymizeUser(Long userId) {
        log.info("匿名化用户数据: userId={}", userId);

        userRepository.findById(userId).ifPresent(user -> {
            String anonymizedPrefix = "anonymized_" + userId + "_";
            user.setUsername(anonymizedPrefix + UUID.randomUUID().toString().substring(0, 8));
            user.setEmail(anonymizedPrefix + "email");
            user.setPhone(null);
            user.setIsActive(false);
            userRepository.save(user);
            log.info("用户基本信息已匿名化: userId={}", userId);
        });

        // 清理会话
        userSessionRepository.findByUserId(userId).forEach(session -> {
            session.setIpAddress("0.0.0.0");
            session.setUserAgent("Anonymized");
            session.setBrowser("Unknown");
            session.setOs("Unknown");
            userSessionRepository.save(session);
        });

        // 清理登录日志中的IP
        loginLogRepository.findByUserIdOrderByLoginTimeDesc(userId).forEach(loginLog -> {
            loginLog.setIp("0.0.0.0");
            loginLogRepository.save(loginLog);
        });

        // 清理系统日志中的IP
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getTenantId() != null) {
                systemLogRepository.findByTenantId(user.getTenantId()).stream()
                        .filter(l -> userId.equals(l.getUserId()))
                        .forEach(sysLog -> {
                            sysLog.setIp("0.0.0.0");
                            systemLogRepository.save(sysLog);
                        });
            }
        });

        log.info("用户数据匿名化完成: userId={}", userId);
    }

    /**
     * 获取数据保留报告
     *
     * @return 数据保留报告
     */
    public Map<String, Object> getDataRetentionReport() {
        log.info("生成数据保留报告");

        Map<String, Object> report = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        report.put("reportTime", now.toString());

        Map<String, Object> systemLogs = new LinkedHashMap<>();
        systemLogs.put("total", systemLogRepository.count());
        report.put("systemLogs", systemLogs);

        Map<String, Object> testResults = new LinkedHashMap<>();
        testResults.put("total", testResultRepository.count());
        report.put("testResults", testResults);

        Map<String, Object> loginLogs = new LinkedHashMap<>();
        loginLogs.put("total", loginLogRepository.count());
        report.put("loginLogs", loginLogs);

        Map<String, Object> users = new LinkedHashMap<>();
        users.put("total", userRepository.count());
        report.put("users", users);

        Map<String, Object> sessions = new LinkedHashMap<>();
        sessions.put("total", userSessionRepository.count());
        report.put("sessions", sessions);

        return report;
    }

    // ==================== Private Helper Methods ====================

    private Map<String, Object> agentToMap(Agent agent) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", agent.getId());
        map.put("name", agent.getName());
        map.put("description", agent.getDescription());
        map.put("status", agent.getStatus());
        map.put("category", agent.getCategory());
        map.put("createdAt", agent.getCreatedAt());
        return map;
    }

    private Map<String, Object> testCaseToMap(AgentTestCase tc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tc.getId());
        map.put("name", tc.getTestName());
        map.put("agentId", tc.getAgentId());
        map.put("status", tc.getStatus());
        map.put("createdAt", tc.getCreatedAt());
        return map;
    }

    private Map<String, Object> executionToMap(AgentTestExecution exec) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", exec.getId());
        map.put("agentId", exec.getAgentId());
        map.put("status", exec.getStatus());
        map.put("createdAt", exec.getCreatedAt());
        return map;
    }

    private Map<String, Object> testResultToMap(AgentTestResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", result.getId());
        map.put("executionId", result.getExecutionId());
        map.put("agentId", result.getAgentId());
        map.put("status", result.getStatus());
        map.put("errorMessage", result.getErrorMessage());
        map.put("createdAt", result.getCreatedAt());
        return map;
    }

    private Map<String, Object> systemLogToMap(SystemLog sysLog) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sysLog.getId());
        map.put("module", sysLog.getModule());
        map.put("operation", sysLog.getOperation());
        map.put("method", sysLog.getMethod());
        map.put("isSuccess", sysLog.getIsSuccess());
        map.put("createdAt", sysLog.getCreatedAt());
        return map;
    }

    private Map<String, Object> loginLogToMap(LoginLog loginLog) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", loginLog.getId());
        map.put("username", loginLog.getUsername());
        map.put("ip", loginLog.getIp());
        map.put("status", loginLog.getStatus());
        map.put("loginTime", loginLog.getLoginTime());
        return map;
    }

    private Map<String, Object> sessionToMap(UserSession s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("sessionId", s.getSessionId());
        map.put("ipAddress", s.getIpAddress());
        map.put("browser", s.getBrowser());
        map.put("os", s.getOs());
        map.put("loginTime", s.getLoginTime());
        map.put("status", s.getStatus());
        return map;
    }
}
