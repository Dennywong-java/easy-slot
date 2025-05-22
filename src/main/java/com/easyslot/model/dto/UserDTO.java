package com.easyslot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String email;
    private String password;
    private String name;
    private NotificationSettingDTO notificationSettings;
    private BrowserSettingDTO browserSettings;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSettingDTO {
        private boolean emailEnabled;
        private String emailAddress;
        private String emailPassword;
        private boolean smsEnabled;
        private String phoneNumber;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowserSettingDTO {
        private String browserType;
        private boolean headless;
        private String binaryLocation;
    }
} 