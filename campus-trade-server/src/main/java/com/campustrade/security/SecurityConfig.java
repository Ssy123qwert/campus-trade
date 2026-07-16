package com.campustrade.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置
 *
 * 安全策略：
 * - 无状态（无 HttpSession），完全依赖 JWT
 * - 公开接口（登录/注册/文件访问/商品浏览）无需认证
 * - 管理后台接口（/api/admin/**）需要管理员角色
 * - 其余接口需要有效 JWT Token
 * - 方法级注解 @PreAuthorize 用于精细权限控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 启用 @PreAuthorize 注解
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /** 不需要认证即可访问的路径 */
    private static final String[] PUBLIC_URLS = {
            "/api/user/login",
            "/api/user/register",
            "/api/product/list",        // 商品列表（POST）
            "/api/product/categories",  // 分类列表
            "/uploads/**",
            "/", "/index.html", "/assets/**", "/favicon.ico",  // SPA 静态资源
            "/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**"  // Swagger
    };

    /** 公开的 GET 请求（商品浏览、文件下载） */
    private static final String[] PUBLIC_GET_URLS = {
            "/api/product/detail",
            "/api/product/similar",
            "/api/user/public/**",      // 公开用户信息
            "/api/file/image/**",
            "/api/file/thumb/**",       // 缩略图
            "/api/file/videos/**",      // 视频文件
            "/api/announcement/**",
            "/ws/**"            // WebSocket 握手端点
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ===== 1. 禁用 CSRF（纯 API 服务，不需要） =====
            .csrf(csrf -> csrf.disable())

            // ===== 2. 无状态会话 =====
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ===== 3. 请求授权规则 =====
            .authorizeHttpRequests(auth -> auth
                    // 完全公开的接口
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    // 公开的 GET 请求（商品浏览）
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
                    // 管理后台接口需要 ADMIN 角色
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // 其余所有接口需要认证
                    .anyRequest().authenticated()
            )

            // ===== 4. 添加 JWT 过滤器（在用户名密码过滤器之前执行） =====
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ===== 5. 禁用 Spring Security 默认的登录表单 =====
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * 密码编码器（BCrypt）
     * 用于用户注册时加密密码、登录时校验密码
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager（用于登录认证）
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
