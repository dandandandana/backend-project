package com.dan.dandexiangmu.controller;

import com.dan.dandexiangmu.dto.response.Result;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

/**
 * 纯 PUT 请求测试控制器（无任何校验依赖，仅测试接口连通性）
 */
@RestController
@RequestMapping("/api/test") // 独立测试路径，不影响业务接口
@CrossOrigin(
        origins = "http://localhost:5173", // 与前端地址一致
        allowedHeaders = "*", // 允许所有请求头
        methods = {RequestMethod.PUT}, // 明确允许 PUT 方法
        allowCredentials = "true"
)
public class TestController {

    /**
     * 测试 PUT 请求：接收简单 JSON 参数，直接返回（无业务逻辑）
     */
    @PutMapping("/put-test")
    public Result<TestPutDTO> testPutRequest(@RequestBody TestPutDTO request) {
        // 直接返回接收的参数，验证 PUT 请求是否能被正确接收和响应
        return Result.success(request);
    }

    /**
     * 简单 DTO（无任何校验注解，避免依赖 javax.validation）
     */
    @Data // 仅用 Lombok 的 @Data 简化 getter/setter，无其他依赖
    public static class TestPutDTO {
        private String id;       // 测试字段1
        private String content;  // 测试字段2
        private String message;  // 测试字段3
    }
}