package com.aiagent.engine.graph;

import com.aiagent.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * SSRF (Server-Side Request Forgery) 防护验证器
 *
 * 从 GraphExecutor 中提取的 URL 安全性验证逻辑，
 * 用于防止通过 HTTP 节点发起对内网地址和元数据服务的请求。
 */
@Slf4j
@Component
public class SsrfValidator {

    /**
     * 验证 HTTP 节点 URL 安全性，防止 SSRF 攻击
     *
     * 检查项：
     * 1. 仅允许 http/https 协议
     * 2. URL 必须包含主机名
     * 3. 禁止访问内网地址（loopback、site-local、link-local、any-local）
     * 4. 禁止访问云元数据服务（169.254.x.x）
     *
     * @param url 待验证的 URL
     * @throws BusinessException 如果 URL 不安全
     */
    public void validateHttpUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new BusinessException("HTTP节点仅允许http/https协议: " + scheme);
            }
            String host = uri.getHost();
            if (host == null) {
                throw new BusinessException("HTTP节点URL缺少主机名: " + url);
            }
            // 检查内网地址
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() ||
                address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                throw new BusinessException("HTTP节点不允许访问内网地址: " + host);
            }
            // 检查169.254.x.x (云元数据)
            if (host.startsWith("169.254.")) {
                throw new BusinessException("HTTP节点不允许访问元数据服务: " + host);
            }
        } catch (UnknownHostException e) {
            throw new BusinessException("HTTP节点无法解析主机名: " + url);
        }
    }
}
