package com.dan.dandexiangmu.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 邮件发送测试类（验证 SMTP 配置是否有效）
 */
@SpringBootTest  // 启动 Spring 上下文，才能注入 JavaMailSender
public class MailTest {

    // 注入 Spring 自动配置的 JavaMailSender（无需手动创建）
    @Resource
    private JavaMailSender javaMailSender;

    @Test  // JUnit 5 测试注解，执行该方法即可
    public void testSendMail() {
        try {
            // 构建简单邮件（纯文本，无附件）
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("2292159426@qq.com");  // 发件人（必须和 application.properties 中 username 完全一致）
            message.setTo("z2432331392@163.com");  // 替换为你的测试邮箱（可以是同一个 QQ 邮箱，也可以是其他邮箱）
            message.setSubject("测试邮件-志丹的小窝");  // 邮件标题
            message.setText("这是一封测试邮件，收到说明 SMTP 配置完全正常！");  // 邮件内容

            // 发送邮件
            javaMailSender.send(message);
            System.out.println("测试邮件发送成功！请查收收件邮箱～");
        } catch (Exception e) {
            System.err.println("测试邮件发送失败！错误原因：" + e.getMessage());
            e.printStackTrace();  // 打印详细错误日志，方便排查问题
        }
    }
}