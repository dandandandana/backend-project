package com.dan.dandexiangmu.service.impl;

import com.dan.dandexiangmu.constants.Constants;
import com.dan.dandexiangmu.dto.request.*;
import com.dan.dandexiangmu.dto.response.LoginResponse;
import com.dan.dandexiangmu.dto.response.Result;
import com.dan.dandexiangmu.dto.response.UserInfoResponse;
import com.dan.dandexiangmu.entity.User;
import com.dan.dandexiangmu.mapper.UserMapper;
import com.dan.dandexiangmu.service.UserService;
import com.dan.dandexiangmu.util.JwtUtil;
import com.dan.dandexiangmu.util.MailUtil;
import com.dan.dandexiangmu.util.RedisUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final PasswordEncoder passwordEncoder;
    private final MailUtil mailUtil;
    private final JwtUtil jwtUtil;

    // 从配置文件注入上传路径，默认值兼容原有配置（但建议在application.yml中明确配置）
    @Value("${upload.avatar.path:./upload/avatars}")
    private String avatarUploadPath;

    // 允许的图片后缀（双重校验，更安全）
    private static final String[] ALLOWED_SUFFIXES = {".jpg", ".jpeg", ".png"};
    // 最大文件大小（10MB）
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // ========================== 注册方法（无修改）==========================
    @Override
    @Transactional
    public Result<Void> register(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail();
        String code = registerRequest.getCode();
        String password = registerRequest.getPassword();
        String nickname = registerRequest.getNickname();

        // 1. 验证验证码
        String redisCode = redisUtil.getCode(email);
        if (redisCode == null || !redisCode.equals(code)) {
            return Result.codeError();
        }

        // 2. 邮箱查重
        int count = userMapper.countByEmail(email);
        if (count > 0) {
            return Result.emailExist();
        }

        // 3. 密码加密（BCrypt不可逆加密）
        String encodedPassword = passwordEncoder.encode(password);

        // 4. 构建User实体（默认昵称用邮箱前缀）
        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setNickname(nickname == null || nickname.trim().isEmpty()
                ? email.split("@")[0]
                : nickname.trim());

        // 5. 保存用户到数据库
        int rows = userMapper.insert(user);
        if (rows != 1) {
            return Result.registerFail();
        }

        // 6. 注册成功，清理Redis中的验证码（避免重复使用）
        redisUtil.deleteCode(email);

        return Result.success();
    }

    // ========================== 发送注册验证码方法（无修改）==========================
    @Override
    public Result<Void> sendRegisterCode(String email) {
        // 1. 后端二次校验邮箱格式
        if (!StringUtils.hasText(email) || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return Result.paramError("邮箱格式不正确");
        }

        // 2. 校验邮箱是否已注册（已注册不允许发送注册验证码）
        int count = userMapper.countByEmail(email);
        if (count > 0) {
            return Result.emailExist();
        }

        // 3. 防止1分钟内频繁发送（Redis存储发送标记）
        String sendFlagKey = "email:send:flag:" + email;
        if (redisUtil.hasKey(sendFlagKey)) {
            return Result.fail(
                    Constants.PARAM_ERROR_CODE,
                    "验证码发送过于频繁，请1分钟后再试"
            );
        }

        try {
            // 4. 生成6位随机验证码
            String code = mailUtil.generateCode();

            // 5. 验证码存入Redis（5分钟过期，Constants中配置）
            redisUtil.setCode(email, code);

            // 6. 存储发送标记（1分钟过期）
            redisUtil.set(sendFlagKey, "1", 1, TimeUnit.MINUTES);

            // 7. 调用邮件工具类发送验证码
            mailUtil.sendRegisterCodeMail(email, code);

            return Result.success();
        } catch (Exception e) {
            // 捕获邮件发送异常（如SMTP配置错误、网络问题）
            return Result.serverError("验证码发送失败：" + e.getMessage());
        }
    }

    // ========================== 登录方法（无修改）==========================
    @Override
    public Result<LoginResponse> login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        // 1. 后端二次校验参数格式
        if (!StringUtils.hasText(email) || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return Result.<LoginResponse>paramError("邮箱格式不正确");
        }
        if (!StringUtils.hasText(password) || password.length() < Constants.PASSWORD_MIN_LENGTH
                || password.length() > Constants.PASSWORD_MAX_LENGTH) {
            return Result.<LoginResponse>paramError("密码长度必须为6-20位");
        }

        // 2. 根据邮箱查询用户（不存在则登录失败）
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            return Result.<LoginResponse>fail(Constants.PARAM_ERROR_CODE, "邮箱或密码错误");
        }

        // 3. BCrypt密码比对（明文密码 vs 数据库加密密码）
        boolean passwordMatch = passwordEncoder.matches(password, user.getPassword());
        if (!passwordMatch) {
            return Result.<LoginResponse>fail(Constants.PARAM_ERROR_CODE, "邮箱或密码错误");
        }

        try {
            // 4. 生成JWT Token（包含用户ID、邮箱、昵称）
            String token = jwtUtil.generateToken(user);

            // 【新增】将 Token 存入 Redis，Key 为 `token:用户ID`，过期时间与 JWT 一致（7200秒 = 2小时）
            String redisKey = "token:" + user.getId();
            redisUtil.set(redisKey, token, 7200, TimeUnit.SECONDS); // 调用 RedisUtil 的 set 方法，设置过期时间

            // 5. 存入Security上下文（后续接口可直接获取当前登录用户）
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
            );

            // 6. 构建登录响应DTO（返回Token和用户基本信息，不含敏感数据）
            LoginResponse loginResponse = new LoginResponse(
                    token,
                    user.getEmail(),
                    user.getNickname(),
                    user.getId(),
                    user.getAvatar(),
                    user.getGender(),
                    user.getBirthday(),
                    user.getSignature(),
                    user.getEmailVerified()
            );

            return Result.success(loginResponse);
        } catch (Exception e) {
            return Result.<LoginResponse>serverError("登录失败：" + e.getMessage());
        }
    }

    // ========================== 修改个人资料（无修改）==========================
    @Override
    public Result<LoginResponse> updateProfile(Long userId, UpdateProfileRequest request) {
        // 新增日志：打印入参，确认userId和昵称是否正确
        logger.info("开始修改个人资料：userId={}，新昵称={}", userId, request.getNickname());

        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.error("修改失败：用户不存在，userId={}", userId);
            return Result.<LoginResponse>fail(Constants.PARAM_ERROR_CODE, "用户不存在");
        }

        // 2. 更新昵称（如果请求中包含有效昵称）
        String newNickname = request.getNickname();
        if (StringUtils.hasText(newNickname)) {
            newNickname = newNickname.trim();
            // 校验昵称长度（避免无效修改）
            if (newNickname.length() > 20) {
                logger.error("修改失败：昵称长度超过20位，nickname={}", newNickname);
                return Result.<LoginResponse>paramError("昵称长度不能超过20位");
            }
            // 若昵称未变化，直接返回成功（避免无效SQL执行）
            if (newNickname.equals(user.getNickname())) {
                logger.info("昵称未变化，无需修改：userId={}，当前昵称={}", userId, newNickname);
                LoginResponse response = new LoginResponse(
                        null, // 不返回Token
                        user.getEmail(),
                        user.getNickname(),
                        user.getId(),
                        user.getAvatar(),
                        user.getGender(),
                        user.getBirthday(),
                        user.getSignature(),
                        user.getEmailVerified()
                );
                return Result.success(response);
            }
            // 设置新昵称到User对象（适配Mapper的参数要求）
            user.setNickname(newNickname);
            try {
                // 调用Mapper修改（不改动Mapper，适配原参数）
                int rows = userMapper.updateNicknameById(user);
                logger.info("SQL执行结果：影响行数={}", rows); // 关键日志：确认是否修改成功

                if (rows != 1) {
                    logger.error("修改失败：SQL影响行数为{}，可能是字段不匹配", rows);
                    return Result.<LoginResponse>serverError("资料更新失败");
                }
            } catch (Exception e) {
                // 捕获SQL执行异常（比如update_time字段不存在）
                logger.error("修改失败：SQL执行异常", e);
                return Result.<LoginResponse>serverError("资料更新失败：" + e.getMessage());
            }
        }

        // 3. 构建响应（返回更新后的用户信息）
        LoginResponse response = new LoginResponse(
                null, // 不返回Token（Token无需更新）
                user.getEmail(),
                user.getNickname(),
                user.getId(),
                user.getAvatar(),
                user.getGender(),
                user.getBirthday(),
                user.getSignature(),
                user.getEmailVerified()
        );
        logger.info("修改个人资料成功：userId={}，最终昵称={}", userId, user.getNickname());
        return Result.success(response);
    }

    // ========================== 上传头像（核心强化：日志打印+异常优化）==========================
    @Override
    public Result<LoginResponse> uploadAvatar(Long userId, MultipartFile file) {
        // ########## 强制打印所有关键信息，不遗漏任何环节 ##########
        logger.info("=== 头像上传【终极排查】开始 ===");
        logger.info("1. 基础信息：userId={}，原始文件名={}", userId, file.getOriginalFilename());
        logger.info("2. 配置路径：avatarUploadPath={}", avatarUploadPath);
        logger.info("3. 系统user.dir={}", System.getProperty("user.dir"));
        logger.info("4. 文件信息：大小={}KB，ContentType={}", file.getSize() / 1024, file.getContentType());

        // 1. 校验文件非空
        if (file.isEmpty()) {
            logger.error("=== 上传失败：文件为空 ===");
            return Result.<LoginResponse>paramError("请选择图片文件");
        }

        // 2. 校验文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            logger.error("=== 上传失败：文件过大 ===");
            return Result.<LoginResponse>paramError("文件大小不能超过10MB");
        }

        // 3. 校验文件格式
        String suffix = getFileSuffix(file.getOriginalFilename());
        if (!isAllowedSuffix(suffix)) {
            logger.error("=== 上传失败：格式不支持 ===");
            return Result.<LoginResponse>paramError("仅支持JPG、PNG格式");
        }

        // 4. 生成唯一文件名
        String fileName = UUID.randomUUID().toString() + suffix;
        logger.info("5. 生成唯一文件名：{}", fileName);

        // 5. 强制构建绝对路径（不依赖系统解析，避免歧义）
        File uploadDir = new File(avatarUploadPath);
        String uploadDirAbs = uploadDir.getAbsolutePath(); // 强制获取绝对路径
        File destFile = new File(uploadDirAbs, fileName);
        String destFileAbs = destFile.getAbsolutePath(); // 最终文件绝对路径（核心！）
        logger.info("6. 强制打印：文件将保存到【{}】", destFileAbs); // 这里一定会打印真实路径！

        // 6. 创建目录（强制创建，打印结果）
        if (!uploadDir.exists()) {
            logger.info("7. 目录不存在，尝试创建：{}", uploadDirAbs);
            boolean mkdirSuccess = uploadDir.mkdirs();
            logger.info("8. 目录创建结果：{}", mkdirSuccess);
            if (!mkdirSuccess) {
                logger.error("=== 上传失败：目录创建失败 ===");
                return Result.<LoginResponse>serverError("存储目录创建失败");
            }
        }

        try {
            // 7. 保存文件（强制打印保存前后状态）
            logger.info("9. 开始保存文件：{}", destFileAbs);
            file.transferTo(destFile);
            logger.info("10. 文件保存操作执行完毕！");

            // ########## 强制校验文件状态（这部分日志必须打印！）##########
            logger.info("📌 关键校验：文件绝对路径={}", destFileAbs);
            logger.info("📌 关键校验：文件是否存在={}", destFile.exists());
            logger.info("📌 关键校验：文件大小={}KB", destFile.exists() ? destFile.length() / 1024 : 0);
            logger.info("📌 关键校验：文件是否可读={}", destFile.exists() ? destFile.canRead() : false);

            // 8. 若文件不存在，直接抛出异常（强制暴露问题）
            if (!destFile.exists() || destFile.length() == 0) {
                String errorMsg = "文件保存后不存在！真实路径：" + destFileAbs;
                logger.error("=== 上传失败：{} ===", errorMsg);
                return Result.<LoginResponse>serverError(errorMsg);
            }

            // 9. 更新数据库（原有逻辑）
            User user = userMapper.selectById(userId);
            if (user == null) {
                logger.error("=== 上传失败：用户不存在 ===");
                return Result.<LoginResponse>fail(Constants.PARAM_ERROR_CODE, "用户不存在");
            }
            String avatarUrl = "/upload/avatars/" + fileName;
            user.setAvatar(avatarUrl);
            int updateRows = userMapper.updateAvatarById(user);
            logger.info("11. 数据库更新影响行数：{}", updateRows);

            // 10. 响应结果
            LoginResponse response = new LoginResponse(null, user.getEmail(), user.getNickname(), user.getId(), user.getAvatar(),                        user.getGender(),
                    user.getBirthday(),
                    user.getSignature(),
                    user.getEmailVerified());
            logger.info("=== 头像上传【终极排查】完成 ===");
            return Result.success(response);

        } catch (IOException e) {
            logger.error("=== 上传失败：IO异常 ===", e);
            return Result.<LoginResponse>serverError("文件上传失败：" + e.getMessage() + "，真实路径：" + destFileAbs);
        }
    }

    // ========================== 获取用户信息（无修改）==========================
    @Override
    public Result<UserInfoResponse> getUserInfo(Long userId) {
        // 1. 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.<UserInfoResponse>fail(Constants.PARAM_ERROR_CODE, "用户不存在");
        }

        // 2. 邮箱脱敏处理（保护隐私，只显示部分字符）
        String email = user.getEmail();
        if (email.contains("@")) {
            String[] emailParts = email.split("@");
            String prefix = emailParts[0];
            // 只显示前3位和后2位（例如：abc****@qq.com）
            if (prefix.length() > 5) {
                email = prefix.substring(0, 3) + "****" + prefix.substring(prefix.length() - 2) + "@" + emailParts[1];
            }
        }

        // 3. 构建响应DTO
        UserInfoResponse response = new UserInfoResponse(
                user.getId(),
                email,
                user.getNickname(),
                user.getAvatar(),
                user.getCreateTime()
        );

        return Result.success(response);
    }

    /**
     * 辅助方法：获取文件后缀（处理无后缀/大小写问题）
     */
    private String getFileSuffix(String originalFilename) {
        if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
            logger.warn("文件名无后缀，默认使用.png");
            return ".png";
        }
        // 截取后缀并转小写，避免大小写差异（如.JPG和.jpg）
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * 辅助方法：校验文件后缀是否允许
     */
    private boolean isAllowedSuffix(String suffix) {
        for (String allowedSuffix : ALLOWED_SUFFIXES) {
            if (allowedSuffix.equals(suffix)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public Result<LoginResponse> updateProfileFull(Long userId, @Valid UpdateProfileFullRequest request) {
        logger.info("开始完善个人资料：userId={}，请求参数={}", userId, request);

        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.error("完善资料失败：用户不存在，userId={}", userId);
            return Result.<LoginResponse>fail(Constants.PARAM_ERROR_CODE, "用户不存在");
        }

        try {
            // 2. 调用Mapper更新资料（只更新非空参数）
            int rows = userMapper.updateProfileFull(
                    userId,
                    request.getNickname(),
                    request.getGender(),
                    request.getBirthday(),
                    request.getSignature()
            );
            logger.info("资料更新SQL影响行数：{}", rows);

            // 3. 查询更新后的用户信息（用于返回给前端）
            User updatedUser = userMapper.selectById(userId);

            // 4. 构建响应DTO
            LoginResponse response = new LoginResponse(
                    null,
                    updatedUser.getEmail(),
                    updatedUser.getNickname(),
                    updatedUser.getId(),
                    updatedUser.getAvatar(),
                    updatedUser.getGender(),
                    updatedUser.getBirthday(),
                    updatedUser.getSignature(),
                    updatedUser.getEmailVerified()
            );
            // 补充新增字段到响应（如果LoginResponse没有这些字段，需要新增getter/setter）
            response.setGender(updatedUser.getGender());
            response.setBirthday(updatedUser.getBirthday());
            response.setSignature(updatedUser.getSignature());

            logger.info("个人资料完善成功：userId={}", userId);
            return Result.success(response);
        } catch (Exception e) {
            logger.error("完善资料失败：SQL执行异常", e);
            return Result.<LoginResponse>serverError("资料更新失败：" + e.getMessage());
        }
    }
    @Override
    public Result<Void> changePassword(Long userId, ChangePasswordRequest request) {
        logger.info("开始修改密码：userId={}", userId);

        // 1. 校验参数一致性（新密码 == 确认密码）
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            logger.error("修改密码失败：新密码与确认密码不一致");
            return Result.paramError("新密码与确认密码不一致");
        }

        // 2. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.error("修改密码失败：用户不存在，userId={}", userId);
            return Result.fail(Constants.PARAM_ERROR_CODE, "用户不存在");
        }

        // 3. 校验旧密码是否正确（BCrypt比对：明文旧密码 vs 数据库加密密码）
        boolean oldPwdMatch = passwordEncoder.matches(request.getOldPassword(), user.getPassword());
        if (!oldPwdMatch) {
            logger.error("修改密码失败：旧密码错误，userId={}", userId);
            return Result.paramError("旧密码错误");
        }

        // 4. 校验新密码是否与旧密码相同（避免重复修改）
        if (request.getOldPassword().equals(request.getNewPassword())) {
            logger.error("修改密码失败：新密码与旧密码相同，userId={}", userId);
            return Result.paramError("新密码不能与旧密码相同");
        }

        try {
            // 5. 加密新密码（BCrypt不可逆加密）
            String encodedNewPwd = passwordEncoder.encode(request.getNewPassword());

            // 6. 更新数据库密码
            int rows = userMapper.updatePasswordById(userId, encodedNewPwd);
            logger.info("密码更新SQL影响行数：{}", rows);

            if (rows != 1) {
                logger.error("修改密码失败：SQL执行失败，影响行数={}", rows);
                return Result.serverError("密码更新失败");
            }

            logger.info("密码修改成功：userId={}", userId);
            return Result.success();
        } catch (Exception e) {
            logger.error("修改密码失败：SQL执行异常", e);
            return Result.serverError("密码更新失败：" + e.getMessage());
        }
    }
    @Override
    public Result<Void> sendVerifyEmail(Long userId) {
        logger.info("开始发送邮箱验证邮件：userId={}", userId);

        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.error("发送验证邮件失败：用户不存在，userId={}", userId);
            return Result.fail(Constants.PARAM_ERROR_CODE, "用户不存在");
        }

        // 2. 校验邮箱是否已验证（已验证则无需重复发送）
        if (user.getEmailVerified() != null && user.getEmailVerified() == 1) {
            logger.error("发送验证邮件失败：邮箱已验证，userId={}", userId);
            return Result.paramError("邮箱已验证，无需重复操作");
        }

        // 3. 防止1分钟内频繁发送（Redis限流）
        String sendFlagKey = "email:verify:flag:" + user.getEmail();
        if (redisUtil.hasKey(sendFlagKey)) {
            logger.error("发送验证邮件失败：发送过于频繁，userId={}", userId);
            return Result.fail(Constants.PARAM_ERROR_CODE, "验证码发送过于频繁，请1分钟后再试");
        }

        try {
            // 4. 生成6位随机验证码
            String verifyCode = mailUtil.generateCode();
            logger.info("生成邮箱验证验证码：{}，userId={}", verifyCode, userId);

            // 5. 验证码存入Redis（5分钟过期）
            String redisCodeKey = "email:verify:code:" + user.getEmail();
            redisUtil.set(redisCodeKey, verifyCode, 5, TimeUnit.MINUTES);

            // 6. 存储发送标记（1分钟过期）
            redisUtil.set(sendFlagKey, "1", 1, TimeUnit.MINUTES);

            // 7. 调用MailUtil发送验证邮件（需在MailUtil中新增发送模板）
            mailUtil.sendVerifyEmailMail(user.getEmail(), verifyCode);

            logger.info("邮箱验证邮件发送成功：userId={}，email={}", userId, user.getEmail());
            return Result.success();
        } catch (Exception e) {
            logger.error("发送验证邮件失败：", e);
            return Result.serverError("验证码发送失败：" + e.getMessage());
        }
    }

    // ########## 新增：验证邮箱验证码 ##########
    @Override
    public Result<Void> verifyEmail(Long userId, String email, String code) {
        logger.info("开始验证邮箱验证码：userId={}，email={}", userId, email);

        // 1. 校验参数
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            logger.error("验证失败：邮箱或验证码不能为空");
            return Result.paramError("邮箱或验证码不能为空");
        }

        // 2. 校验用户是否存在，且邮箱匹配
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.error("验证失败：用户不存在，userId={}", userId);
            return Result.fail(Constants.PARAM_ERROR_CODE, "用户不存在");
        }
        if (!email.equals(user.getEmail())) {
            logger.error("验证失败：邮箱与用户绑定邮箱不一致，userId={}", userId);
            return Result.paramError("邮箱与账号绑定邮箱不一致");
        }

        // 3. 校验邮箱是否已验证
        if (user.getEmailVerified() != null && user.getEmailVerified() == 1) {
            logger.error("验证失败：邮箱已验证，userId={}", userId);
            return Result.paramError("邮箱已验证，无需重复操作");
        }

        // 4. 从Redis获取验证码（5分钟过期）
        String redisCodeKey = "email:verify:code:" + email;
        String redisCode = redisUtil.get(redisCodeKey);
        if (redisCode == null) {
            logger.error("验证失败：验证码已过期，userId={}", userId);
            return Result.paramError("验证码已过期，请重新获取");
        }
        if (!redisCode.equals(code)) {
            logger.error("验证失败：验证码错误，userId={}", userId);
            return Result.paramError("验证码错误");
        }

        try {
            // 5. 更新邮箱验证状态为1（已验证）
            int rows = userMapper.updateEmailVerified(userId, 1);
            logger.info("邮箱验证状态更新SQL影响行数：{}", rows);

            if (rows != 1) {
                logger.error("验证失败：SQL执行失败，影响行数={}", rows);
                return Result.serverError("邮箱验证失败");
            }

            // 6. 验证成功，删除Redis中的验证码
            redisUtil.delete(redisCodeKey);

            logger.info("邮箱验证成功：userId={}，email={}", userId, email);
            return Result.success();
        } catch (Exception e) {
            logger.error("验证失败：SQL执行异常", e);
            return Result.serverError("邮箱验证失败：" + e.getMessage());
        }
    }
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId); // 复用你现有的 selectById 方法
    }
}