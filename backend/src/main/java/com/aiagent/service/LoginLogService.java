package com.aiagent.service;

import com.aiagent.entity.LoginLog;
import com.aiagent.repository.LoginLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    /**
     * 记录登录/登录失败日志
     */
    public void recordLogin(String username, Long userId, String status, String message, HttpServletRequest request) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setUserId(userId);
        loginLog.setLoginType("LOGIN_FAIL".equals(status) ? "LOGIN_FAIL" : "LOGIN");
        loginLog.setIp(getClientIp(request));
        loginLog.setBrowser(parseBrowser(request));
        loginLog.setOs(parseOs(request));
        loginLog.setStatus(status);
        loginLog.setMessage(message);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLogRepository.save(loginLog);
    }

    /**
     * 记录登出日志
     */
    public void recordLogout(Long userId, HttpServletRequest request) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setLoginType("LOGOUT");
        loginLog.setIp(getClientIp(request));
        loginLog.setStatus("SUCCESS");
        loginLog.setLoginTime(LocalDateTime.now());
        loginLogRepository.save(loginLog);
    }

    /**
     * 分页查询登录日志
     */
    public Page<LoginLog> getLoginLogs(Pageable pageable) {
        return loginLogRepository.findAll(pageable);
    }

    /**
     * 查询用户的登录日志
     */
    public java.util.List<LoginLog> getUserLoginLogs(Long userId) {
        return loginLogRepository.findByUserIdOrderByLoginTimeDesc(userId);
    }

    /**
     * 统计指定时间范围内某用户的登录失败次数
     */
    public long countFailedLogins(String username, LocalDateTime since) {
        return loginLogRepository.countByUsernameAndLoginTimeAfterAndStatus(username, since, "FAIL");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String parseBrowser(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        return "Other";
    }

    private String parseOs(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "Other";
    }
}
