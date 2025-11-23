package com.dan.dandexiangmu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword; // 旧密码

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须为6-20位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).*$", message = "新密码必须包含字母和数字")
    private String newPassword; // 新密码

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword; // 确认密码


}