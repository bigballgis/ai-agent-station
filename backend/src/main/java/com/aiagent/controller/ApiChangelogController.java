package com.aiagent.controller;

import com.aiagent.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API 变更日志控制器
 *
 * 提供 API 版本变更历史，帮助开发者了解 API 的演进过程。
 * 数据来源: classpath 下的 api-changelog.md 文件。
 */
@Slf4j
@RestController
@RequestMapping("/v1/api-changelog")
@RequiredArgsConstructor
@Tag(name = "API 变更日志", description = "API 版本变更历史查询")
public class ApiChangelogController {

    /**
     * 获取 API 变更日志（Markdown 格式）
     */
    @GetMapping(produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "获取 API 变更日志（Markdown）", description = "返回完整的 API 变更日志 Markdown 内容")
    public String getChangelogMarkdown() {
        try {
            ClassPathResource resource = new ClassPathResource("api-changelog.md");
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("读取 api-changelog.md 失败: {}", e.getMessage());
            return "# API Changelog\n\nUnable to load changelog.";
        }
    }

    /**
     * 获取 API 变更日志（JSON 格式）
     */
    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取 API 变更日志（JSON）", description = "返回结构化的 API 变更日志 JSON 数据")
    public Result<Map<String, Object>> getChangelogJson() {
        Map<String, Object> changelog = new LinkedHashMap<>();

        try {
            ClassPathResource resource = new ClassPathResource("api-changelog.md");
            String markdown;
            try (InputStream is = resource.getInputStream()) {
                markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            changelog.put("format", "markdown");
            changelog.put("content", markdown);
            changelog.put("generatedAt", LocalDateTime.now().toString());

            // 解析版本列表
            List<Map<String, Object>> versions = parseVersions(markdown);
            changelog.put("versions", versions);

        } catch (IOException e) {
            log.warn("读取 api-changelog.md 失败: {}", e.getMessage());
            changelog.put("error", "Unable to load changelog");
        }

        return Result.success(changelog);
    }

    /**
     * 从 Markdown 内容中解析版本列表
     */
    private List<Map<String, Object>> parseVersions(String markdown) {
        List<Map<String, Object>> versions = new ArrayList<>();
        String[] lines = markdown.split("\n");

        String currentVersion = null;
        List<String> currentChanges = null;

        for (String line : lines) {
            // 匹配 ### vX.X.X 格式的版本标题
            if (line.trim().startsWith("### v")) {
                // 保存上一个版本
                if (currentVersion != null) {
                    Map<String, Object> versionEntry = new LinkedHashMap<>();
                    versionEntry.put("version", currentVersion);
                    versionEntry.put("changes", currentChanges);
                    versions.add(versionEntry);
                }

                // 开始新版本
                currentVersion = line.trim().substring(4).split("\\s+")[0];
                currentChanges = new ArrayList<>();
            } else if (currentVersion != null && line.trim().startsWith("- ")) {
                // 收集变更条目
                currentChanges.add(line.trim().substring(2));
            }
        }

        // 保存最后一个版本
        if (currentVersion != null) {
            Map<String, Object> versionEntry = new LinkedHashMap<>();
            versionEntry.put("version", currentVersion);
            versionEntry.put("changes", currentChanges);
            versions.add(versionEntry);
        }

        return versions;
    }
}
