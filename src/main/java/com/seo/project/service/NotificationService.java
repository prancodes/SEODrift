package com.seo.project.service;

import com.seo.project.model.User;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.seo.project.model.Notification;
import com.seo.project.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    public NotificationService(JavaMailSender mailSender, NotificationRepository notificationRepository, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendViralOutlierAlerts(User user, List<Map<String, Object>> outliers) {
        if (outliers == null || outliers.isEmpty()) return;
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject("🔥 Viral Outlier Detected: Hot Topic in Your Niche!");
            
            Context context = new Context();
            context.setVariable("userName", user.getName() != null ? user.getName() : "Creator");
            context.setVariable("outliers", outliers);
            context.setVariable("baseUrl", baseUrl);
            
            String html = templateEngine.process("email/viral-outlier", context);
            
            helper.setText(html, true);
            
            // Save to DB
            Notification dbNotification = Notification.builder()
                .user(user)
                .title("🔥 Viral Outlier Detected: Hot Topic in Your Niche!")
                .message("We detected " + outliers.size() + " viral outliers in your competitor's recent uploads.")
                .type("COMPETITOR_UPLOAD")
                .isRead(false)
                .build();
            notificationRepository.save(dbNotification);

            // Send Email if enabled
            if (user.getEmailNotificationsEnabled() != null && user.getEmailNotificationsEnabled()) {
                mailSender.send(message);
                log.info("Sent viral outlier email alert to {}", user.getEmail());
            } else {
                log.info("User {} has email notifications disabled. Skipping email for viral outlier.", user.getEmail());
            }
            
        } catch (Exception e) {
            log.error("Failed to process viral outlier notification for {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    @Async
    public void sendConsistencyNudge(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject("⏰ Time to post? Keep your momentum going!");
            
            Context context = new Context();
            context.setVariable("userName", user.getName() != null ? user.getName() : "Creator");
            context.setVariable("baseUrl", baseUrl);
            
            String html = templateEngine.process("email/consistency-nudge", context);
                    
            helper.setText(html, true);
            
            // Save to DB
            Notification dbNotification = Notification.builder()
                .user(user)
                .title("⏰ Time to post? Keep your momentum going!")
                .message("It looks like you haven't published a video in a while. Consistency is key for YouTube growth.")
                .type("PUBLISH_REMINDER")
                .isRead(false)
                .build();
            notificationRepository.save(dbNotification);

            // Send Email if enabled
            if (user.getEmailNotificationsEnabled() != null && user.getEmailNotificationsEnabled()) {
                mailSender.send(message);
                log.info("Sent consistency nudge email to {}", user.getEmail());
            } else {
                log.info("User {} has email notifications disabled. Skipping consistency nudge email.", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to process consistency nudge notification for {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
