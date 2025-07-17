package com.kaishui.entitlement.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    // 从 application.properties 中获取发件人地址
    // 注意: 对于IP白名单模式，您可能需要一个自定义属性，如 app.mail.from=...
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送简单的纯文本邮件。
     * @param to 收件人
     * @param subject 主题
     * @param text 内容
     */
    @Async // 异步执行
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
            log.info("Simple email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}", to, e);
        }
    }

    /**
     * 发送包含HTML内容和附件的邮件。
     * @param to 收件人
     * @param subject 主题
     * @param htmlContent HTML 格式的内容
     * @param attachmentPath 附件的绝对路径 (可选)
     */
    @Async // 异步执行
    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlContent, String attachmentPath) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            // MimeMessageHelper 是一个强大的帮助类, true 表示这是一个 multipart message
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true 表示邮件内容为HTML

            // 添加附件
            if (attachmentPath != null && !attachmentPath.isBlank()) {
                FileSystemResource file = new FileSystemResource(new File(attachmentPath));
                helper.addAttachment(Objects.requireNonNull(file.getFilename()), file);
            }

            javaMailSender.send(mimeMessage);
            log.info("HTML email with attachment sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email with attachment to {}", to, e);
        }
    }
}