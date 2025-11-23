package com.dan.dandexiangmu.config;

// 创建配置类（如 com.dan.dandexiangmu.config.CorsConfig.java）
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 1. 允许前端域名（本地开发为http://localhost:5173，生产替换为实际域名）
        config.addAllowedOrigin("http://localhost:5173");
        // 2. 允许所有请求方法（GET/POST/PUT/DELETE等）
        config.addAllowedMethod("*");
        // 3. 关键：允许Authorization请求头（前端传递Token的头）
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type"); // 基础头也需允许
        // 4. 允许携带Cookie（若需登录态关联Cookie，可选）
        config.setAllowCredentials(true);
        // 5. 预检请求缓存时间（减少OPTIONS请求次数）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 6. 对所有接口生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}