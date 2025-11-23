package com.dan.dandexiangmu.util;

import com.dan.dandexiangmu.constants.Constants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    // 关键修改：将 StringRedisTemplate 改为 RedisTemplate<String, String>
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // 存储邮箱验证码（逻辑不变）
    public void setCode(String email, String code) {
        redisTemplate.opsForValue().set(
                Constants.EMAIL_CODE_PREFIX + email,
                code,
                Constants.CODE_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );
    }

    // 获取邮箱验证码（逻辑不变）
    public String getCode(String email) {
        return redisTemplate.opsForValue().get(Constants.EMAIL_CODE_PREFIX + email);
    }

    // 删除邮箱验证码（逻辑不变）
    public void deleteCode(String email) {
        redisTemplate.delete(Constants.EMAIL_CODE_PREFIX + email);
    }

    // 判断key是否存在（逻辑不变）
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 存储普通键值对（逻辑不变）
    public void set(String key, String value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }
    public void set(String key, String value, long expireSeconds) {
        redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}