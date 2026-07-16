package com.campustrade.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器（每个请求执行一次）
 *
 * 工作流程：
 * 1. 从请求头 Authorization 中提取 Bearer Token
 * 2. 验证 Token 有效性（签名、过期、黑名单）
 * 3. 解析 userId + role，构造 Authentication 对象
 * 4. 存入 SecurityContextHolder，后续 Controller 可直接获取用户信息
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    /** Authorization 请求头名称 */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    /** Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 提取 Token
        String token = extractToken(request);

        // 2. 如果有 Token 且有效，设置认证信息
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);

            if (userId != null) {
                // 加载用户信息
                CustomUserDetails userDetails = userDetailsService.loadUserById(userId);

                // 创建认证令牌（已认证状态）
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,    // principal：当前用户
                                null,            // credentials：不需要密码
                                userDetails.getAuthorities()  // 权限列表
                        );

                // 存入 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 Bearer Token
     * @param request HTTP 请求
     * @return Token 字符串，如果没有则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
