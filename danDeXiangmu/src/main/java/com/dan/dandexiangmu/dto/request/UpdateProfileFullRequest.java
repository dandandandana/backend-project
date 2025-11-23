package com.dan.dandexiangmu.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileFullRequest {
    @NotNull(message = "userId不能为空") // 必须传递
    private Long userId; // 新增：把 userId 放到 DTO 里
    @Size(max = 20, message = "昵称长度不能超过20位")
    private String nickname; // 可选：可修改昵称

    @Pattern(regexp = "^(男|女|保密)?$", message = "性别只能是男、女、保密")
    private String gender; // 可选：男/女/保密

    private LocalDate birthday; // 可选：生日（前端传YYYY-MM-DD格式）

    @Size(max = 200, message = "个人签名长度不能超过200位")
    private String signature; // 可选：个人签名
}