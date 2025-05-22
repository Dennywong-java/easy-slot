package com.easyslot.service.notification;

import com.easyslot.config.model.Notifications;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Service for sending email notifications.
 * Handles configuration, authentication, and delivery of notifications.
 */
@Service
public class NotificationService implements NotificationServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final Notifications config;
    private Session session;

    public NotificationService(Notifications config) {
        this.config = config;
        initializeMailSession();
    }

    private void initializeMailSession() {
        if (config == null || config.getGmail() == null) {
            logger.warn("Mail configuration is incomplete, notification service may not work properly");
            return;
        }
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        
        // Add debug options
        props.put("mail.debug", "true");

        try {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        config.getGmail().getEmail(),
                        config.getGmail().getAppPassword()
                    );
                }
            });
            
            logger.info("Mail session initialized for: {}", config.getGmail().getEmail());
        } catch (Exception e) {
            logger.error("Failed to initialize mail session: {}", e.getMessage());
        }
    }

    /**
     * Sends an email notification with the specified subject and content.
     * Error handling is included to prevent notification failures from affecting the main process.
     *
     * @param subject Email subject
     * @param content Email body content
     */
    @Override
    public void sendNotification(String subject, String content) {
        if (session == null || config == null || config.getGmail() == null || 
            config.getGmail().getEmail() == null || 
            config.getGmail().getAppPassword() == null) {
            logger.warn("Notification configuration is incomplete or session not initialized, skipping notification");
            return;
        }
        
        try {
            // Print send configuration for debugging (without password)
            logger.info("Sending email notification: from={}, to={}, subject={}", 
                config.getGmail().getEmail(), 
                config.getGmail().getRecipientEmail(),
                subject);
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getGmail().getEmail()));
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(config.getGmail().getRecipientEmail())
            );
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
            logger.info("Email notification sent successfully: {}", subject);
        } catch (MessagingException e) {
            logger.error("Failed to send email notification: {}", e.getMessage());
        }
    }
} 