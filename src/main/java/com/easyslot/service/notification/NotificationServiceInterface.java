package com.easyslot.service.notification;

/**
 * Notification service interface for sending notifications
 */
public interface NotificationServiceInterface {
    /**
     * Send notification
     * @param subject Notification subject
     * @param content Notification content
     */
    void sendNotification(String subject, String content);
} 