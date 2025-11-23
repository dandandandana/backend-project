package com.dan.dandexiangmu.util;

import com.dan.dandexiangmu.constants.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Component
public class MailUtil {

    @Resource
    private  JavaMailSender javaMailSender;

    @Value("${spring.mail.username}") // 从配置文件读取发件人邮箱
    private String fromEmail;

    // 生成6位随机验证码
    public String generateCode() {
        return String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
    }

    // 发送注册验证码邮件
    public void sendRegisterCodeMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(Constants.MAIL_FROM); // 发件人（和application.yml的username一致）
        message.setTo(email);                // 收件人
        message.setSubject("【志丹的小窝】注册验证码"); // 邮件标题
        // 关键修改：添加场景说明，避免内容过于模板化
        message.setText("您正在注册“志丹的小窝”账号，验证码是：" + code + "，有效期5分钟，请勿泄露给他人～");
        javaMailSender.send(message);
    }
    public void sendVerifyEmailMail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("【邮箱验证】请完成您的账号验证");
        message.setText("您的邮箱验证验证码为：" + code + "，5分钟内有效，请尽快完成验证～\n" +
                "若不是您本人操作，请忽略此邮件。");
        javaMailSender.send(message);
    }
}