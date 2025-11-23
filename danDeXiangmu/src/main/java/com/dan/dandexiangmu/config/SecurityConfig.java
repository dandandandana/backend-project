package com.dan.dandexiangmu.config;

import com.dan.dandexiangmu.service.security.JwtAuthenticationFilter;
import com.dan.dandexiangmu.service.security.UserDetailsServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Security 核心配置类（整合JWT认证+跨域+无状态会话）
 */
@Configuration
@EnableWebSecurity // 启用Web安全机制
@EnableMethodSecurity // 支持方法级权限控制（如@PreAuthorize）
public class SecurityConfig {

    // 注入用户详情服务（查询数据库验证用户）
    @Resource
    private UserDetailsServiceImpl userDetailsService;

    // 注入JWT认证过滤器（拦截请求并验证Token）
    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // 注入Spring提供的跨域过滤器（解决前端跨域请求问题）
    @Resource
    private CorsFilter corsFilter;

    /**
     * 密码加密器：使用BCrypt算法（不可逆加密，安全可靠）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证提供者：关联用户详情服务和密码加密器，用于用户认证
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // 关联用户详情服务（从数据库查询用户）
        authProvider.setUserDetailsService(userDetailsService);
        // 关联密码加密器（验证密码时自动解密匹配）
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 认证管理器：Spring Security核心组件，负责统一认证逻辑
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 安全过滤器链：核心配置（接口权限、跨域、JWT、会话管理等）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 先应用跨域过滤器（必须在JWT过滤器之前，解决跨域请求拦截问题）
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)

                // 2. 关闭CSRF防护（前后端分离场景，无Session，无需CSRF）
                .csrf(csrf -> csrf.disable())

                // 3. 配置会话管理：无状态模式（JWT认证不需要Session）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )


                // 4. 接口权限控制：放行公开接口，其他需认证
                .authorizeHttpRequests(auth -> auth
                        // 放行OPTIONS预检请求（浏览器复杂请求（如PUT/带Authorization头）必过）
                        .requestMatchers(request -> "OPTIONS".equals(request.getMethod())).permitAll()
                        // 放行登录、注册、发送验证码接口（公开访问）
                        .requestMatchers("/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/send-register-code",
                                "/upload/avatars/**",
                                // ====== Swagger 相关路径（已修正）======
                                "/swagger-ui/**",

                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/swagger-ui.html"
                                // ===================================
                        ).permitAll()
                        // 其他所有接口必须认证（携带有效JWT Token）
                        .anyRequest().authenticated()
                )

                // 5. 关联认证提供者（用于用户密码登录认证）
                .authenticationProvider(authenticationProvider())

                // 6. 添加JWT认证过滤器（在用户名密码过滤器之前执行，优先验证Token）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}