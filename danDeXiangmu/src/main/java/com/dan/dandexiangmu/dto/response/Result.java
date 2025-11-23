package com.dan.dandexiangmu.dto.response;

import com.dan.dandexiangmu.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * 统一响应结果（支持泛型错误返回，修复类型不兼容问题）
 * @param <T> 数据类型（成功时返回的数据）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@CrossOrigin
public class Result<T> {
    private int code;      // 状态码
    private String message; // 提示信息
    private T data;        // 成功时返回的数据（失败时可null）

    // ------------------------------ 成功响应（原有，不变）------------------------------
    public static <T> Result<T> success(T data) {
        return new Result<>(Constants.SUCCESS_CODE, Constants.SUCCESS_MSG, data);
    }

    public static Result<Void> success() {
        return new Result<>(Constants.SUCCESS_CODE, Constants.SUCCESS_MSG, null);
    }

    // ------------------------------ 错误响应（修改为泛型返回）------------------------------
    /**
     * 参数错误（支持泛型，适配任意返回类型）
     */
    public static <T> Result<T> paramError(String detail) {
        return new Result<>(Constants.PARAM_ERROR_CODE, Constants.PARAM_ERROR_MSG + "：" + detail, null);
    }

    /**
     * 验证码错误（支持泛型）
     */
    public static <T> Result<T> codeError() {
        return new Result<>(Constants.PARAM_ERROR_CODE, Constants.CODE_ERROR_MSG, null);
    }

    /**
     * 邮箱已注册（支持泛型）
     */
    public static <T> Result<T> emailExist() {
        return new Result<>(Constants.PARAM_ERROR_CODE, Constants.EMAIL_EXIST_MSG, null);
    }

    /**
     * 注册失败（支持泛型）
     */
    public static <T> Result<T> registerFail() {
        return new Result<>(Constants.SERVER_ERROR_CODE, Constants.REGISTER_FAIL_MSG, null);
    }

    /**
     * 服务器错误（支持泛型）
     */
    public static <T> Result<T> serverError(String detail) {
        return new Result<>(Constants.SERVER_ERROR_CODE, Constants.SERVER_ERROR_MSG + "：" + detail, null);
    }

    /**
     * 通用失败响应（支持泛型）
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}