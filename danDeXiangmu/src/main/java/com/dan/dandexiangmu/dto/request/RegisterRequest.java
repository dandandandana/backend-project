package com.dan.dandexiangmu.dto.request;

import com.dan.dandexiangmu.constants.Constants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * 注册请求参数DTO（基于Constants常量校验）
 */
@Data
@CrossOrigin
public class RegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(
            min = Constants.PASSWORD_MIN_LENGTH,
            max = Constants.PASSWORD_MAX_LENGTH,
            message = "密码长度必须为" + Constants.PASSWORD_MIN_LENGTH + "-" + Constants.PASSWORD_MAX_LENGTH + "位"
    )
    private String password;

    @Size(
            max = Constants.NICKNAME_MAX_LENGTH,
            message = "昵称最长" + Constants.NICKNAME_MAX_LENGTH + "位"
    )
    private String nickname;

    @NotBlank(message = "验证码不能为空")
    private String code;
}