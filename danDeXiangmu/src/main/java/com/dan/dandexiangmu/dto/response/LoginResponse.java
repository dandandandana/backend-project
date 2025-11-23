package com.dan.dandexiangmu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDate;

/**
 * 登录成功响应DTO（返回Token+用户基本信息）
 */
@Data
@AllArgsConstructor
@CrossOrigin
public class LoginResponse {
    private String token;        // JWT认证Token
    private String email;        // 用户邮箱（登录账号）
    private String nickname;     // 用户昵称（用于前端显示）
    private Long userId;
    private String avatar;
    private String gender; // 男/女/保密
    private LocalDate birthday; // 生日
    private String signature; // 个人签名
    private Integer emailVerified;
}