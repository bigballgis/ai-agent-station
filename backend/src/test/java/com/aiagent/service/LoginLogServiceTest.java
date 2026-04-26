package com.aiagent.service;

import com.aiagent.entity.LoginLog;
import com.aiagent.repository.LoginLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LoginLogService 单元测试
 * 测试登录日志记录、IP解析、浏览器识别等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("登录日志服务测试")
class LoginLogServiceTest {

    @Mock
    private LoginLogRepository loginLogRepository;

    @InjectMocks
    private LoginLogService loginLogService;

    private static void withRequest(org.springframework.mock.web.MockHttpServletRequest request, Runnable runnable) {
        try {
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                    new org.springframework.web.context.request.ServletRequestAttributes(request)
            );
            runnable.run();
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @DisplayName("记录登录日志 - 成功")
    void testRecordLogin() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> {
            LoginLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");

        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "admin".equals(log.getUsername()) &&
                log.getUserId() == 1L &&
                "LOGIN".equals(log.getLoginType()) &&
                "SUCCESS".equals(log.getStatus()) &&
                "登录成功".equals(log.getMessage()) &&
                log.getLoginTime() != null
        ));
    }

    @Test
    @DisplayName("记录登录失败日志 - loginType应为LOGIN_FAIL")
    void testRecordLogin_Fail() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");

        withRequest(request, () -> loginLogService.recordLogin("admin", null, "LOGIN_FAIL", "密码错误"));

        verify(loginLogRepository).save(argThat(log ->
                "LOGIN_FAIL".equals(log.getLoginType()) &&
                "LOGIN_FAIL".equals(log.getStatus())
        ));
    }

    @Test
    @DisplayName("记录登出日志 - 成功")
    void testRecordLogout() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");

        withRequest(request, () -> loginLogService.recordLogout(1L));

        verify(loginLogRepository).save(argThat(log ->
                log.getUserId() == 1L &&
                "LOGOUT".equals(log.getLoginType()) &&
                "SUCCESS".equals(log.getStatus())
        ));
    }

    @Test
    @DisplayName("获取客户端IP - 从X-Forwarded-For获取")
    void testGetClientIp_FromXForwardedFor() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        request.setRemoteAddr("192.168.1.100");

        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "10.0.0.1".equals(log.getIp())
        ));
    }

    @Test
    @DisplayName("获取客户端IP - 从X-Real-IP获取")
    void testGetClientIp_FromXRealIp() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.0.0.2");
        request.setRemoteAddr("192.168.1.100");

        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "10.0.0.2".equals(log.getIp())
        ));
    }

    @Test
    @DisplayName("获取客户端IP - 从RemoteAddr获取")
    void testGetClientIp_FromRemoteAddr() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.1");
        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "172.16.0.1".equals(log.getIp())
        ));
    }

    @Test
    @DisplayName("获取客户端IP - X-Forwarded-For为unknown时降级")
    void testGetClientIp_UnknownForwardedFor() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "10.0.0.5");
        request.setRemoteAddr("172.16.0.1");
        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "10.0.0.5".equals(log.getIp())
        ));
    }

    @Test
    @DisplayName("解析浏览器 - Chrome")
    void testParseBrowser_Chrome() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0");
        request.setRemoteAddr("192.168.1.100");
        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "Chrome".equals(log.getBrowser())
        ));
    }

    @Test
    @DisplayName("解析浏览器 - Firefox")
    void testParseBrowser_Firefox() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 Firefox/121.0");
        request.setRemoteAddr("192.168.1.100");
        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "Firefox".equals(log.getBrowser())
        ));
    }

    @Test
    @DisplayName("解析浏览器 - Edge")
    void testParseBrowser_Edge() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 Edg/120.0.0.0");
        request.setRemoteAddr("192.168.1.100");
        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "Edge".equals(log.getBrowser())
        ));
    }

    @Test
    @DisplayName("解析浏览器 - Safari")
    void testParseBrowser_Safari() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 Safari/605.1.15");
        request.setRemoteAddr("192.168.1.100");
        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "Safari".equals(log.getBrowser())
        ));
    }

    @Test
    @DisplayName("解析浏览器 - 未知浏览器")
    void testParseBrowser_Unknown() {
        when(loginLogRepository.save(any(LoginLog.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        withRequest(request, () -> loginLogService.recordLogin("admin", 1L, "SUCCESS", "登录成功"));

        verify(loginLogRepository).save(argThat(log ->
                "Unknown".equals(log.getBrowser())
        ));
    }
}
