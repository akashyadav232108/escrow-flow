package com.escrowflow.service;

import com.escrowflow.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final TemplateEngine templateEngine;

    public EmailService(
            JavaMailSender mailSender, 
            AppProperties appProperties,
            TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
        this.templateEngine = templateEngine;
    }

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        String subject = purpose.equals("EMAIL_VERIFICATION") 
            ? "Verify Your Email - Escrow Flow" 
            : "Reset Your Password - Escrow Flow";
        
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("otp", otp);
        context.setVariable("validity", appProperties.getOtp().getTtlMinutes() + " minutes");
        
        if (purpose.equals("EMAIL_VERIFICATION")) {
            context.setVariable("heading", "Email Verification Code");
            context.setVariable("message", "Use the code below to verify your email address:");
        } else {
            context.setVariable("heading", "Password Reset Code");
            context.setVariable("message", "Use the code below to reset your password:");
        }
        
        String body = templateEngine.process("emails/otp-email", context);
        sendHtmlEmail(toEmail, subject, body);
    }

    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "Welcome to Escrow Flow!";
        
        Context context = new Context();
        context.setVariable("userName", userName);
        
        String body = templateEngine.process("emails/welcome-email", context);
        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(appProperties.getEmail().getFrom(), appProperties.getEmail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
