package com.aiagent.mcp;

/**
 * JSON-RPC 2.0 与 MCP 方法名常量（与 <a href="https://modelcontextprotocol.io">Model Context Protocol</a> 规范中的命名一致）。
 * <p>
 * 说明：行业常说的「新版 MCP / MCP2」多指规范<strong>按日期发布的新修订</strong>，而非另有一套「MCP2 协议名」。
 * 平台通过可配置的 {@code protocolVersion} 字符串在 {@code initialize} 中与远端协商。
 */
public final class McpJsonRpcConstants {

    public static final String JSON_RPC_2_0 = "2.0";

    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_TOOLS_LIST = "tools/list";
    public static final String METHOD_TOOLS_CALL = "tools/call";
    /** 在 initialize 成功后，客户端应发送的 MCP 标准通知。 */
    public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

    private McpJsonRpcConstants() {
    }
}
