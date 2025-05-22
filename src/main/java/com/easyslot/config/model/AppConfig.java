package com.easyslot.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Application configuration model class
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    private MonitoringConfig monitoring;
    private BrowserConfig browser;
    private NotificationConfig notification;
    private DebugConfig debug;
    private Map<String, UserConfig> users;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MonitoringConfig {
        private int checkIntervalSeconds = 30;
        private int errorRetryIntervalSeconds = 60;
        private int maxRetries = 3;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrowserConfig {
        private String type = "chrome";
        private boolean headless = true;
        private String binaryLocation;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NotificationConfig {
        private boolean enabled = true;
        private boolean emailEnabled = true;
        private boolean smsEnabled = false;
        private String smtpHost = "smtp.gmail.com";
        private int smtpPort = 587;
        private String emailAddress;
        private GmailConfig gmail;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GmailConfig {
            private boolean enabled = true;
            private String email;
            private String appPassword;
            private String recipientEmail;
        }
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DebugConfig {
        private boolean enabled = false;
        private boolean saveScreenshots = true;
        private boolean saveHtml = true;
        private boolean sendEmailNotifications = false;
        private long emailIntervalSeconds = 300; // 5 minutes interval
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserConfig {
        private String email;
        private String password;
        private AppointmentConfig appointment;
        private NotificationConfig notification;
        private BrowserConfig browser;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppointmentConfig {
        private String location;
        private String startDate;
        private String endDate;
        private String[] preferredCities;
        private boolean autoBook = false;
        private String ivrNumber;
    }
} 