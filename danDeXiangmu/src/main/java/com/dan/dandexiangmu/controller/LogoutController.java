package com.dan.dandexiangmu.controller;

import com.dan.dandexiangmu.dto.response.Result;
import com.dan.dandexiangmu.util.JwtUtil;
import com.dan.dandexiangmu.util.RedisUtil;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class LogoutController {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisUtil redisUtil;

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        // 1. 从请求头中提取 Token（格式：Bearer {token}）
        String token = authorization.replace("Bearer ", "").trim();

        // 2. 解析 Token 获取用户 ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail(401, "无效的 Token");
        }

        // 3. 删除 Redis 中对应的 Token
        String redisKey = "token:" + userId;
        redisUtil.delete(redisKey);

        return Result.success();
    }
}