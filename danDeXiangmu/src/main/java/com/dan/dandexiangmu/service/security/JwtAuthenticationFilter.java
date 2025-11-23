package com.dan.dandexiangmu.service.security;

import com.dan.dandexiangmu.config.JwtConfig;
import com.dan.dandexiangmu.entity.User;
import com.dan.dandexiangmu.util.JwtUtil;
import com.dan.dandexiangmu.util.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器（每次请求都会拦截，验证Token）
 */
@Component
@CrossOrigin
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private JwtConfig jwtConfig;
    @Resource
    private UserDetailsServiceImpl userDetailsService; // 后续编写，用于查询用户
    @Resource
    private RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. 从请求头中获取Token（格式：Bearer xxxxxx）
            String token = getTokenFromRequest(request);

            String path = request.getRequestURI();

            // ✅ 跳过 Swagger 和公开路径，不进行 JWT 验证
            if (path.startsWith("/swagger-ui") ||
                    path.startsWith("/v3/api-docs") ||
                    path.equals("/swagger-ui.html")) {
                filterChain.doFilter(request, response);
                return;
            }
            logger.info("【JWT过滤器】提取到的Token：" + (token == null ? "null" : "已获取（长度：" + token.length() + "）")); // 日志1：是否拿到Token

            if (StringUtils.hasText(token)) {
                // 2. 从Token中解析用户ID
                Long userId = jwtUtil.getUserIdFromToken(token);
                logger.info("【JWT过滤器】从Token解析出的用户ID：" + userId); // 日志2：解析的userId是否正确

                // 3. 根据用户ID查询用户信息（UserDetailsServiceImpl）
                User user = (User) userDetailsService.loadUserById(userId);
                logger.info("【JWT过滤器】根据userId查询到的用户邮箱：" + (user == null ? "null" : user.getEmail())); // 日志3：是否查到用户

                // 4. 验证Token是否有效（JWT本身合法 + Redis中存在该Token）
                boolean tokenValid = jwtUtil.validateToken(token, user);
                logger.info("【JWT过滤器】JWT Token本身是否有效：" + tokenValid); // 日志4：Token基础校验结果

                if (tokenValid) {
                    // 【新增】Redis校验：查询Redis中是否存在该用户的Token，且与当前Token一致
                    String redisKey = "token:" + userId; // 和登录时存入的Key格式一致
                    String redisToken = redisUtil.get(redisKey); // 从Redis获取Token
                    logger.info("【JWT过滤器】Redis中查询到的Token：" + (redisToken == null ? "null" : "已获取（长度：" + redisToken.length() + "）")); // 日志5：Redis Token是否存在

                    if (redisToken == null || !redisToken.equals(token)) {
                        logger.warn("【JWT过滤器】Redis Token与请求Token不一致或不存在，直接放行");
                        // Redis中无该Token（用户已退出），直接放行（后续Security拦截）
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 5. 把用户信息存入Security上下文（后续接口可直接获取当前用户）
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("【JWT过滤器】Security上下文设置成功，用户已认证"); // 日志6：认证成功标识
                }
            }
        } catch (Exception e) {
            // Token无效或解析失败，不存入上下文（后续会被拦截）
            logger.error("【JWT过滤器】认证失败：" + e.getMessage(), e); // 新增：打印完整异常栈（关键！）
        }

        // 继续执行后续过滤器（放行或拦截由Security配置决定）
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Token（去掉前缀Bearer）
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtConfig.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtConfig.getPrefix() + " ")) {
            return bearerToken.substring(jwtConfig.getPrefix().length() + 1);
        }
        return null;
    }
}