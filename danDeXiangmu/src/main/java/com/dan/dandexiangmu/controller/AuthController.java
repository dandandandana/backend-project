package com.dan.dandexiangmu.controller;

import com.dan.dandexiangmu.dto.request.LoginRequest;
import com.dan.dandexiangmu.dto.request.RegisterRequest;
import com.dan.dandexiangmu.dto.request.UpdateProfileRequest;
import com.dan.dandexiangmu.dto.response.LoginResponse;
import com.dan.dandexiangmu.dto.response.Result;
import com.dan.dandexiangmu.dto.response.UserInfoResponse;
import com.dan.dandexiangmu.entity.User;
import com.dan.dandexiangmu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.dan.dandexiangmu.config.CorsConfig;
/**
 * 认证控制器（登录、注册、验证码接口）
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class AuthController {

    private final UserService userService;

    // 注册接口（已实现）
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterRequest registerRequest) {
        return userService.register(registerRequest);
    }

    // 发送注册验证码接口（已实现）
    @GetMapping("/send-register-code")
    public Result<Void> sendRegisterCode(@RequestParam String email) {
        return userService.sendRegisterCode(email);
    }

    // 新增：登录接口（POST请求，接收JSON参数）
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest);
    }

    /**
     * 修改个人资料（仅登录用户可访问）
     */
    @PutMapping("/profile")
    public Result<LoginResponse> updateProfile(@Validated @RequestBody UpdateProfileRequest request) {
        // 从Security上下文获取当前登录用户ID
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.updateProfile(currentUser.getId(), request);
    }

    /**
     * 获取当前登录用户资料（扩展接口，可选）
     */
    @GetMapping("/profile" )
    public Result<LoginResponse> getProfile() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LoginResponse response = new LoginResponse(
                null,
                currentUser.getEmail(),
                currentUser.getNickname(),
                currentUser.getId(),
                currentUser.getAvatar(),
                currentUser.getGender(),
                currentUser.getBirthday(),
                currentUser.getSignature(),
                currentUser.getEmailVerified()
        );
        return Result.success(response);
    }

    @PostMapping("/avatar")
    public Result<LoginResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 从Security上下文获取当前登录用户ID
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.uploadAvatar(currentUser.getId(), file);

    }
    /**
     * 获取当前登录用户的详细信息（包含创建时间等扩展字段）
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo() {
        // 从Security上下文获取当前登录用户
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.getUserInfo(currentUser.getId());
    }
}