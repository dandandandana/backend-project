package com.dan.dandexiangmu.util;

import com.dan.dandexiangmu.config.JwtConfig;
import com.dan.dandexiangmu.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT工具类（生成Token、解析Token、验证Token）
 */
@Component
public class JwtUtil {

    @Resource
    private JwtConfig jwtConfig;

    /**
     * 生成JWT Token（登录成功后调用）
     * @param userDetails Spring Security的用户详情（这里是User实体类）
     * @return 加密后的Token
     */
    public String generateToken(UserDetails userDetails) {
        // 1. 构建JWT的Payload（负载）：存储用户ID、邮箱等非敏感信息
        User user = (User) userDetails;
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtConfig.getExpiration());

        // 2. 生成Token（用密钥签名，确保不被篡改）
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()); // 密钥转成Key对象
        return Jwts.builder()
                .setSubject(user.getId().toString()) // 主题：存储用户ID（唯一标识）
                .claim("email", user.getEmail())     // 额外信息：邮箱
                .claim("nickname", user.getNickname()) // 额外信息：昵称
                .setIssuedAt(now)                    // 签发时间
                .setExpiration(expirationDate)       // 过期时间
                .signWith(key)                       // 签名（用HMAC-SHA算法）
                .compact();
    }

    /**
     * 从Token中解析出用户ID（用于后续业务查询）
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 验证Token是否有效（是否过期、签名是否正确）
     * @param token Token
     * @param userDetails 数据库查询到的用户信息
     * @return true=有效，false=无效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        User user = (User) userDetails;
        Long userId = getUserIdFromToken(token);
        // 验证：用户ID一致 + Token未过期
        return userId.equals(user.getId()) && !isTokenExpired(token);
    }

    /**
     * 解析Token的Payload（负载）
     */
    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token) // 去掉前缀（如Bearer ）
                .getBody();
    }

    /**
     * 检查Token是否过期
     */
    private boolean isTokenExpired(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().before(new Date());
    }
}