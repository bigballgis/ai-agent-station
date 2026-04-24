package com.aiagent.security;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.entity.UserRole;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.repository.RoleRepository;
import com.aiagent.service.AuthService;
import com.aiagent.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final ApiKeyService apiKeyService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;
    private final AiAgentProperties aiAgentProperties;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ApiKeyService apiKeyService,
            UserRoleRepository userRoleRepository, RoleRepository roleRepository,
            @Lazy AuthService authService, AiAgentProperties aiAgentProperties) {
        this.jwtUtil = jwtUtil;
        this.apiKeyService = apiKeyService;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.authService = authService;
        this.aiAgentProperties = aiAgentProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);
            String apiKey = getApiKeyFromRequest(request);

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                // 检查token是否在黑名单中
                if (authService.isTokenBlacklisted(token)) {
                    log.debug("Token已在黑名单中，拒绝访问");
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = jwtUtil.getUsernameFromToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                Long tenantId = jwtUtil.getTenantIdFromToken(token);

                setTenantContext(tenantId);

                // 加载用户真实角色
                List<SimpleGrantedAuthority> authorities = loadUserAuthorities(userId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, username, tenantId),
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (StringUtils.hasText(apiKey) && apiKeyService.validateApiKey(apiKey)) {
                Long tenantId = apiKeyService.getTenantIdByApiKey(apiKey);
                setTenantContext(tenantId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(null, "api-user", tenantId),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_API"))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.warn("无法设置用户认证", e);
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> loadUserAuthorities(Long userId) {
        if (userId == null) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
        try {
            List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
            if (userRoles.isEmpty()) {
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            }
            return userRoles.stream()
                    .map(ur -> roleRepository.findById(ur.getRoleId()))
                    .filter(opt -> opt.isPresent())
                    .map(opt -> "ROLE_" + opt.get().getName().toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("加载用户角色失败，使用默认角色: {}", e.getMessage());
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    private void setTenantContext(Long tenantId) {
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setSchemaName(aiAgentProperties.getTenant().getSchemaPrefix() + tenantId);
        } else {
            TenantContextHolder.setSchemaName(aiAgentProperties.getTenant().getDefaultSchema());
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getApiKeyFromRequest(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        return null;
    }
}
