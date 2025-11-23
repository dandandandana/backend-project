package com.dan.dandexiangmu.controller;

import com.dan.dandexiangmu.dto.request.ChangePasswordRequest;
import com.dan.dandexiangmu.dto.request.UpdateProfileFullRequest;
import com.dan.dandexiangmu.dto.request.UpdateProfileRequest;
import com.dan.dandexiangmu.dto.response.LoginResponse;
import com.dan.dandexiangmu.dto.response.Result;
import com.dan.dandexiangmu.entity.User;
import com.dan.dandexiangmu.service.UserService;
import com.dan.dandexiangmu.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户资料控制器（处理个人信息相关接口）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil; // 复用你现有的 JwtUtil，不修改任何代码

    /**
     * 修改个人资料（简化版）- 保留你原有的 SecurityContext 逻辑
     */
    @PutMapping("/profile")
    public Result<LoginResponse> updateProfile(@Validated @RequestBody UpdateProfileRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.updateProfile(currentUser.getId(), request);
    }

    /**
     * 完善个人资料（完整版）- 从 Token 解析 userId
     */
    @PutMapping("/profile/full")
    public Result<LoginResponse> updateProfileFull(
            @RequestHeader("Authorization") String token, // 从请求头获取 Token
            @Valid @RequestBody UpdateProfileFullRequest request) {
        // 通用逻辑：解析 Token 得到 userId（适配你的 JwtUtil）
        Long userId = getUserIdFromToken(token);
        return userService.updateProfileFull(userId, request);
    }

    /**
     * 修改密码接口 - 从 Token 解析 userId
     */
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = getUserIdFromToken(token);
        return userService.changePassword(userId, request);
    }

    /**
     * 发送邮箱验证邮件接口 - 从 Token 解析 userId（无需前端传 userId）
     */
    @GetMapping("/email/send-verify")
    public Result<Void> sendVerifyEmail(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return userService.sendVerifyEmail(userId);
    }

    /**
     * 验证邮箱验证码接口 - 从 Token 解析 userId（无需前端传 userId）
     */
    @PostMapping("/email/verify")
    public Result<Void> verifyEmail(
            @RequestHeader("Authorization") String token,
            @RequestParam("email") String email,
            @RequestParam("code") String code) {
        Long userId = getUserIdFromToken(token);
        return userService.verifyEmail(userId, email, code);
    }

    /**
     * 获取当前登录用户的详细信息（扩展接口，可选）
     */
    @GetMapping("/info")
    public Result<User> getCurrentUserInfo(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        User user = userService.getUserById(userId); // 需在 UserService 新增：根据 userId 查询用户的方法
        return Result.success(user);
    }

    // ########## 通用工具方法：解析 Token 获取 userId（适配你的 JwtUtil）##########
    private Long getUserIdFromToken(String token) {
        try {
            // 关键：你的 JwtUtil 没有处理 "Bearer " 前缀，这里手动去掉
            if (token.startsWith("Bearer ")) {
                token = token.replace("Bearer ", "");
            }
            // 调用你现有的 JwtUtil 方法解析 userId（从 Subject 中获取）
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            // Token 解析失败（过期、格式错误、签名错误），返回友好提示
            throw new RuntimeException("登录状态失效，请重新登录");
        }
    }
}