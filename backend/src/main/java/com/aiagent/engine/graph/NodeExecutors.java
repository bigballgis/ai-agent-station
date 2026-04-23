package com.aiagent.engine.graph;

import com.aiagent.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

/**
 * 节点执行器集合
 *
 * 从 GraphExecutor 中提取的独立节点类型执行逻辑。
 * 包含 Code 节点和 Delay 节点的执行方法，
 * 这些方法在 GraphExecutor 的主循环和 executeNodeInternal 中被重复使用。
 */
@Slf4j
@Component
public class NodeExecutors {

    /**
     * 执行 Code 节点（JavaScript 代码执行）
     *
     * @param nodeId 节点 ID
     * @param config 节点配置（包含 language 和 code 字段）
     * @param nodeOutputs 上游节点输出（作为变量注入到脚本引擎）
     * @return 代码执行结果字符串
     * @throws BusinessException 如果代码为空、语言不支持或执行失败
     */
    public String executeCodeNode(String nodeId, Map<String, Object> config, Map<String, Object> nodeOutputs) {
        String language = (String) config.getOrDefault("language", "javascript");
        String code = (String) config.get("code");

        if (code == null || code.isBlank()) {
            throw new BusinessException("Code 节点代码为空: " + nodeId);
        }

        String result;

        if ("javascript".equalsIgnoreCase(language) || "js".equalsIgnoreCase(language)) {
            try {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName("js");
                if (engine != null) {
                    // 注入上游节点输出作为变量
                    for (Map.Entry<String, Object> entry : nodeOutputs.entrySet()) {
                        engine.put(entry.getKey(), entry.getValue());
                    }
                    Object evalResult = engine.eval(code);
                    result = evalResult != null ? evalResult.toString() : "";
                } else {
                    log.warn("JavaScript 引擎不可用，Code 节点 {} 跳过执行", nodeId);
                    result = "[JavaScript engine not available]";
                }
            } catch (ScriptException e) {
                throw new BusinessException("Code 节点执行失败: " + e.getMessage(), e);
            }
        } else {
            throw new BusinessException("Code 节点不支持的语言: " + language + " (当前仅支持 javascript)");
        }

        return result;
    }

    /**
     * 执行 Delay 节点（延迟等待）
     *
     * @param nodeId 节点 ID
     * @param config 节点配置（包含 seconds 字段）
     * @return 延迟结果字符串（格式: "delayed_Xs"）
     * @throws BusinessException 如果延迟被中断
     */
    public String executeDelayNode(String nodeId, Map<String, Object> config) {
        Object secondsObj = config.get("seconds");
        int seconds = 1;
        if (secondsObj instanceof Number) {
            seconds = ((Number) secondsObj).intValue();
        } else if (secondsObj instanceof String) {
            try { seconds = Integer.parseInt((String) secondsObj); } catch (NumberFormatException e) { /* use default */ }
        }
        seconds = Math.max(1, Math.min(seconds, 300)); // 限制 1-300 秒（超时上限）

        // 业务需求: Delay 节点需要等待指定时间，保留 Thread.sleep
        log.info("Delay 节点 {} 等待 {} 秒", nodeId, seconds);
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Delay 节点被中断: " + nodeId, e);
        }

        return "delayed_" + seconds + "s";
    }
}
