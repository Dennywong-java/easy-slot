package com.easyslot.config;

import com.easyslot.config.model.AppConfig;
import com.easyslot.config.model.Gmail;
import com.easyslot.config.model.Notifications;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notifications configuration class, provides Notifications Bean
 */
@Configuration
public class NotificationsConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationsConfig.class);
    private final ConfigLoader configLoader;
    
    public NotificationsConfig(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }
    
    /**
     * Create notification configuration Bean
     * 
     * @return Notifications object
     */
    @Bean
    public Notifications notifications() {
        Notifications notifications = new Notifications();
        Gmail gmail = new Gmail();
        
        // Load configuration from AppConfig
        AppConfig appConfig = configLoader.loadConfig();
        
        // Check if Gmail settings exist in configuration
        if (appConfig.getNotification() != null && appConfig.getNotification().getGmail() != null) {
            // Use Gmail configuration directly
            AppConfig.NotificationConfig.GmailConfig gmailConfig = appConfig.getNotification().getGmail();
            gmail.setEnabled(gmailConfig.isEnabled());
            gmail.setEmail(gmailConfig.getEmail());
            gmail.setAppPassword(gmailConfig.getAppPassword());
            gmail.setRecipientEmail(gmailConfig.getRecipientEmail());
            
            logger.info("Gmail configuration loaded, using sender: {}, recipient: {}", 
                        gmail.getEmail(), gmail.getRecipientEmail());
        } else {
            // No notification configuration, use default values
            logger.warn("Gmail configuration not found, using default values");
            gmail.setEnabled(true);
            gmail.setEmail("test@example.com");
            gmail.setAppPassword("password");
            gmail.setRecipientEmail("test@example.com");
        }
        
        notifications.setGmail(gmail);
        return notifications;
    }
} 