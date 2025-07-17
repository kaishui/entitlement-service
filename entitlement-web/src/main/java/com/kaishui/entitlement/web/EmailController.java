package com.kaishui.entitlement.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/send")
    public String sendTestEmail(@RequestParam String to) {
        String subject = "来自 Spring Boot 的测试邮件";
        String htmlBody = "<h1>你好!</h1>"
                + "<p>这是一封通过 <strong>Spring Boot Mail</strong> 发送的HTML邮件。</p>"
                + "<p>它也包含一个附件。</p>";

        // 使用您项目中的 test.csv 文件作为附件
        String attachmentPath = "/Users/kaishui/workspace/entitlement-service/doc/local/test.csv";

        emailService.sendHtmlEmailWithAttachment(to, subject, htmlBody, attachmentPath);

        return "邮件发送请求已提交。请检查邮箱：" + to;
    }
}