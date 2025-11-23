package com.dan.dandexiangmu.service.security;

import com.dan.dandexiangmu.entity.User;
import com.dan.dandexiangmu.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Spring Security用户详情服务（用于查询用户信息进行认证）
 */
@Service
@CrossOrigin
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserMapper userMapper;

    /**
     * 根据用户名（这里是邮箱）查询用户（Spring Security默认调用）
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + email);
        }
        return user; // User已实现UserDetails接口
    }

    /**
     * 根据用户ID查询用户（JWT过滤器中调用）
     */
    public UserDetails loadUserById(Long userId) {
        User user = userMapper.selectById(userId); // 需在UserMapper中新增selectById方法
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + userId);
        }
        return user;
    }
}