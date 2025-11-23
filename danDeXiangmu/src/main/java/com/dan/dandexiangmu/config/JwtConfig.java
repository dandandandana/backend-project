package com.dan.dandexiangmu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置类（从application.yml读取配置）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt") // 对应yml中jwt前缀的配置
public class JwtConfig {
    private String secret;     // JWT密钥（application.yml中配置）
    private long expiration;   // Token过期时间（毫秒）

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    private String header;     // 请求头中Token的Key（如Authorization）
    private String prefix;     // Token前缀（如Bearer）
}