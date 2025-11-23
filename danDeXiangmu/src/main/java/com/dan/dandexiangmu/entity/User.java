package com.dan.dandexiangmu.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户实体类（与数据库 sys_user 表映射）
 */
@Data  // Lombok 注解：自动生成 getter/setter/toString/equals/hashCode 方法
public class User implements UserDetails {  // 实现 UserDetails 适配 Spring Security 认证
    // 1. 对应数据库表字段（字段名驼峰命名，适配 MyBatis 下划线转驼峰配置）
    private Long id;                // 主键ID（对应表中 id）
    private String email;           // 用户邮箱（对应表中 email，唯一）
    private String password;        // 加密后的密码（对应表中 password）
    private String nickname;        // 用户昵称（对应表中 nickname，可空）
    private LocalDateTime createTime;// 创建时间（对应表中 create_time）
    private String avatar;
    private String gender; // 男/女/保密
    private LocalDate birthday; // 生日
    private String signature; // 个人签名
    private Integer emailVerified; // 邮箱是否验证：0-未验证，1-已验证
    // 2. 以下是 UserDetails 接口实现（适配 Spring Security，固定写法，暂时无需修改）
    // 角色权限集合（当前仅登录功能，暂无需角色，返回空集合）
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    // Spring Security 认证时用的「用户名」—— 这里我们用 email 作为登录账号，所以返回 email
    @Override
    public String getUsername() {
        return this.email;
    }

    // 账号是否未过期（默认true，后续可扩展账号有效期逻辑）
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 账号是否未锁定（默认true，后续可扩展账号锁定逻辑）
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 密码是否未过期（默认true，后续可扩展密码有效期逻辑）
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 账号是否启用（默认true，后续可扩展账号禁用逻辑）
    @Override
    public boolean isEnabled() {
        return true;
    }
}