package com.dan.dandexiangmu.constants;

/**
 * 全局常量类（集中管理所有硬编码数据）
 */
public final class Constants {
    public static final String MAIL_FROM = "2292159426@qq.com";

    // Redis 验证码前缀（可选，之前的逻辑中也可以用这个常量）


    // 验证码过期时间（分钟）
      // final修饰，禁止继承
    // 私有构造方法，禁止实例化
    private Constants() {}

    // ========================== 状态码常量 ==========================
    public static final int SUCCESS_CODE = 200;       // 成功状态码
    public static final int PARAM_ERROR_CODE = 400;   // 参数错误状态码
    public static final int UN_AUTH_CODE = 401;       // 未认证（登录失效）状态码
    public static final int SERVER_ERROR_CODE = 500;  // 服务器错误状态码

    // ========================== 消息提示常量 ==========================
    public static final String SUCCESS_MSG = "操作成功";                  // 成功提示
    public static final String PARAM_ERROR_MSG = "参数校验失败";           // 参数错误提示
    public static final String CODE_ERROR_MSG = "验证码错误或已过期";      // 验证码错误提示
    public static final String EMAIL_EXIST_MSG = "该邮箱已被注册";         // 邮箱已注册提示
    public static final String REGISTER_FAIL_MSG = "注册失败，请重试";      // 注册失败提示
    public static final String SERVER_ERROR_MSG = "服务器异常，请稍后重试"; // 服务器错误提示

    // ========================== Redis键前缀常量 ==========================
    public static final String EMAIL_CODE_PREFIX = "email:code:";  // 邮箱验证码Redis键前缀（拼接email形成完整key）

    // ========================== 其他常量 ==========================
    public static final int CODE_EXPIRE_MINUTES = 5;  // 验证码有效期（分钟）
    public static final int PASSWORD_MIN_LENGTH = 6;  // 密码最小长度
    public static final int PASSWORD_MAX_LENGTH = 20; // 密码最大长度
    public static final int NICKNAME_MAX_LENGTH = 50; // 昵称最大长度
}