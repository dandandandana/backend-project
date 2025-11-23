package com.dan.dandexiangmu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户详细信息响应DTO
 */
@Data
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;                // 用户ID
    private String email;           // 邮箱（脱敏处理）
    private String nickname;        // 昵称
    private String avatar;          // 头像URL
    private LocalDateTime createTime; // 账号创建时间
}