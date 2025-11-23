package com.dan.dandexiangmu.dto.request;

import com.dan.dandexiangmu.constants.Constants;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * 修改个人资料请求参数DTO
 */
@Data
@CrossOrigin
public class UpdateProfileRequest {

    @Size(
            max = Constants.NICKNAME_MAX_LENGTH,
            message = "昵称最长" + Constants.NICKNAME_MAX_LENGTH + "位"
    )
    private String nickname; // 仅支持修改昵称（可扩展其他字段）
}