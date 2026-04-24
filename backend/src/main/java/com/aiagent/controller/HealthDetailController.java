package com.aiagent.controller;

import com.aiagent.service.ApplicationHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查详情端点
 *
 * 提供 /actuator/health/detail 路径，返回所有组件的详细健康状态。
 * 与 Spring Boot Actuator 原生 /actuator/health 端点互补，
 * 提供更丰富的应用层健康信息。
 */
@RestController
@RequestMapping("/actuator/health")
@RequiredArgsConstructor
public class HealthDetailController {

    private final ApplicationHealthService applicationHealthService;

    /**
     * 获取详细的健康状态
     * 包含: 数据库、Redis、磁盘空间、LLM 提供商、JWT 密钥、缓存命中率
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        return ResponseEntity.ok(applicationHealthService.getDetailedHealth());
    }
}
