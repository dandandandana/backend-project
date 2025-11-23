package com.dan.dandexiangmu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 配置静态资源访问路径，让前端可以访问上传的头像
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射URL路径到本地文件目录
        String localAvatarPath = "file:D:/manager_new/danDeXiangmu/upload/avatars/";
        registry.addResourceHandler("/upload/avatars/**")
                .addResourceLocations(localAvatarPath);
        System.out.println("静态资源映射：/upload/avatars/** → " + localAvatarPath);
    }
}