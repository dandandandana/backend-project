package com.dan.dandexiangmu.mapper;

import com.dan.dandexiangmu.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 用户Mapper接口（MyBatis数据访问层）
 */
@Mapper  // 标记为MyBatis Mapper接口，Spring Boot会自动扫描
public interface UserMapper {

    /**
     * 根据邮箱查询用户（登录、邮箱查重用）
     * @param email 用户邮箱（登录账号）
     * @return 完整User对象（包含密码、昵称等），无数据返回null
     */
    User selectByEmail(String email);

    /**
     * 新增用户（注册用）
     * @param user 待新增的用户对象（需包含email、password、nickname）
     * @return 影响行数（1=新增成功，0=失败）
     */
    int insert(User user);

    /**
     * 根据邮箱查询用户（登录、邮箱查重用）
     * @param email 用户邮箱（登录账号）
     * @return 完整User对象（包含密码、昵称等），无数据返回null
     */
    int countByEmail(String email);
    User selectById(Long id);
    int updateNicknameById(User user);
    int updateAvatarById(User user);
    int updateProfileFull(@Param("userId") Long userId,
                          @Param("nickname") String nickname,
                          @Param("gender") String gender,
                          @Param("birthday") LocalDate birthday,
                          @Param("signature") String signature);
    // 新增：更新密码
    int updatePasswordById(@Param("userId") Long userId, @Param("newPassword") String newPassword);
    // 新增：更新邮箱验证状态
    int updateEmailVerified(@Param("userId") Long userId, @Param("emailVerified") Integer emailVerified);
}