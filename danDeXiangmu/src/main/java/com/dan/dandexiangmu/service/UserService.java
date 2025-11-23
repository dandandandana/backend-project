package com.dan.dandexiangmu.service;

import com.dan.dandexiangmu.dto.request.*;
import com.dan.dandexiangmu.dto.response.LoginResponse;
import com.dan.dandexiangmu.dto.response.Result;
import com.dan.dandexiangmu.dto.response.UserInfoResponse;
import com.dan.dandexiangmu.entity.User;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    // 注册（已实现）
    Result<Void> register(RegisterRequest registerRequest);
    // 发送注册验证码（已实现）
    Result<Void> sendRegisterCode(String email);
    // 新增：用户登录
    Result<LoginResponse> login(LoginRequest loginRequest);
    Result<LoginResponse> updateProfile(Long userId, UpdateProfileRequest request);
    Result<LoginResponse> uploadAvatar(Long userId, MultipartFile file);
    Result<UserInfoResponse> getUserInfo(Long userId);

    Result<LoginResponse> updateProfileFull(Long userId, @Valid UpdateProfileFullRequest request);

    Result<Void> changePassword(Long userId, ChangePasswordRequest request);

    Result<Void> sendVerifyEmail(Long userId);

    // ########## 新增：验证邮箱验证码 ##########
    Result<Void> verifyEmail(Long userId, String email, String code);

    User getUserById(Long userId);
}