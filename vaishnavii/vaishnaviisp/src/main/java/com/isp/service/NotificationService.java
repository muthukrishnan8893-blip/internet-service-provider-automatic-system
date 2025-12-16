package com.isp.service;

import com.isp.model.Notification;
import com.isp.model.NotificationPreferences;
import com.isp.model.User;
import com.isp.repo.NotificationRepository;
import com.isp.repo.NotificationPreferencesRepository;
import com.isp.repo.UserRepository;

import java.util.List;

/**
 * Comprehensive notification service that handles all notification types
 */
public class NotificationService {
    private NotificationRepository notificationRepo;
    private NotificationPreferencesRepository preferencesRepo;
    private UserRepository userRepo;
    private EmailService emailService;
    private SmsService smsService;

    public NotificationService(NotificationRepository notificationRepo,
                             NotificationPreferencesRepository preferencesRepo,
                             UserRepository userRepo,
                             EmailService emailService,
                             SmsService smsService) {
        this.notificationRepo = notificationRepo;
        this.preferencesRepo = preferencesRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    /**
     * Send a notification through all enabled channels
     */
    public void sendNotification(String userId, String category, String title, String message, String priority) {
        try {
            // Get user and preferences
            java.util.Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                System.err.println("[NOTIFICATION] User not found: " + userId);
                return;
            }
            User user = userOpt.get();

            NotificationPreferences prefs = preferencesRepo.findByUserId(userId);
            
            // Determine which channels to use based on category and preferences
            boolean sendEmail = shouldSendEmail(category, priority, prefs);
            boolean sendBrowser = shouldSendBrowser(category, priority, prefs);
            boolean sendSms = shouldSendSMS(category, priority, prefs);

            // Send through each enabled channel
            if (sendEmail) {
                sendEmailNotification(user, title, message, category);
            }

            if (sendBrowser) {
                saveBrowserNotification(userId, category, title, message, priority);
            }

            if (sendSms && prefs.getPhoneNumber() != null) {
                sendSmsNotification(user, prefs.getPhoneNumber(), title, message, category);
            }

            System.out.println("[NOTIFICATION] Sent to " + user.getUsername() + " | " + title + " | Channels: " + 
                (sendEmail ? "Email " : "") + (sendBrowser ? "Browser " : "") + (sendSms ? "SMS" : ""));

        } catch (Exception e) {
            System.err.println("[NOTIFICATION] Error sending notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send usage alert notification
     */
    public void sendUsageAlert(String userId, int usagePercent, double dataUsedGB, double remainingGB, double totalGB) {
        String title = String.format("Data Usage Alert - %d%% Used", usagePercent);
        String message = String.format(
            "You have used %d%% (%.2f GB) of your %d GB data plan. %.2f GB remaining.",
            usagePercent, dataUsedGB, (int)totalGB, remainingGB
        );

        String priority = usagePercent >= 90 ? "CRITICAL" : usagePercent >= 75 ? "HIGH" : "MEDIUM";
        sendNotification(userId, "USAGE_ALERT", title, message, priority);
    }

    /**
     * Send payment reminder notification
     */
    public void sendPaymentReminder(String userId, double amount, String dueDate) {
        String title = "Payment Reminder";
        String message = String.format(
            "Your payment of $%.2f is due on %s. Please pay to avoid service interruption.",
            amount, dueDate
        );
        sendNotification(userId, "PAYMENT", title, message, "HIGH");
    }

    /**
     * Send low balance alert
     */
    public void sendLowBalanceAlert(String userId, double balance) {
        String title = "Low Balance Alert";
        String message = String.format(
            "Your account balance is low: $%.2f. Please recharge to continue service.",
            balance
        );
        String priority = balance < 10 ? "CRITICAL" : "HIGH";
        sendNotification(userId, "PAYMENT", title, message, priority);
    }

    /**
     * Send ticket update notification
     */
    public void sendTicketUpdate(String userId, String ticketId, String ticketSubject, String updateMessage) {
        String title = "Ticket Updated: " + ticketSubject;
        String message = "Your support ticket #" + ticketId.substring(0, 8) + " has been updated:\n\n" + updateMessage;
        sendNotification(userId, "TICKET", title, message, "MEDIUM");
    }

    /**
     * Send security alert notification
     */
    public void sendSecurityAlert(String userId, String alertTitle, String alertMessage) {
        sendNotification(userId, "SECURITY", alertTitle, alertMessage, "CRITICAL");
    }

    /**
     * Send new device connected alert
     */
    public void sendNewDeviceAlert(String userId, String deviceName, String ipAddress) {
        String title = "New Device Connected";
        String message = String.format(
            "A new device '%s' (IP: %s) has connected to your network.",
            deviceName, ipAddress
        );
        sendNotification(userId, "SECURITY", title, message, "MEDIUM");
    }

    /**
     * Send plan change confirmation
     */
    public void sendPlanChangeConfirmation(String userId, String oldPlan, String newPlan) {
        String title = "Plan Changed Successfully";
        String message = String.format(
            "Your plan has been changed from '%s' to '%s'. The new plan is now active.",
            oldPlan, newPlan
        );
        sendNotification(userId, "SYSTEM", title, message, "LOW");
    }

    // Helper methods for determining which channels to use

    private boolean shouldSendEmail(String category, String priority, NotificationPreferences prefs) {
        if (!prefs.isEmailEnabled()) return false;
        
        switch (category) {
            case "USAGE_ALERT":
                return prefs.isEmailUsageAlerts();
            case "PAYMENT":
                return prefs.isEmailPaymentReminders();
            case "TICKET":
                return prefs.isEmailTicketUpdates();
            case "SECURITY":
                return prefs.isEmailSecurityAlerts();
            default:
                return true;
        }
    }

    private boolean shouldSendBrowser(String category, String priority, NotificationPreferences prefs) {
        if (!prefs.isBrowserEnabled()) return false;
        
        switch (category) {
            case "USAGE_ALERT":
                return prefs.isBrowserUsageAlerts();
            case "PAYMENT":
                return prefs.isBrowserPaymentReminders();
            case "TICKET":
                return prefs.isBrowserTicketUpdates();
            case "SECURITY":
                return prefs.isBrowserSecurityAlerts();
            default:
                return true;
        }
    }

    private boolean shouldSendSMS(String category, String priority, NotificationPreferences prefs) {
        if (!prefs.isSmsEnabled()) return false;
        
        // If critical only mode is enabled, only send critical notifications
        if (prefs.isSmsCriticalOnly() && !"CRITICAL".equals(priority)) {
            return false;
        }
        
        switch (category) {
            case "USAGE_ALERT":
                return prefs.isSmsUsageAlerts();
            case "PAYMENT":
                return prefs.isSmsPaymentReminders();
            case "SECURITY":
                return prefs.isSmsSecurityAlerts();
            default:
                return "CRITICAL".equals(priority);
        }
    }

    private void sendEmailNotification(User user, String title, String message, String category) {
        String emailBody = buildEmailBody(user.getUsername(), title, message, category);
        emailService.sendEmail(user.getEmail(), title, emailBody);
    }

    private void saveBrowserNotification(String userId, String category, String title, String message, String priority) {
        Notification notification = new Notification(userId, "BROWSER", category, title, message, priority);
        notification.setSent(true);
        notificationRepo.save(notification);
    }

    private void sendSmsNotification(User user, String phoneNumber, String title, String message, String category) {
        // Truncate message for SMS (160 character limit)
        String smsMessage = message;
        if (smsMessage.length() > 140) {
            smsMessage = smsMessage.substring(0, 137) + "...";
        }
        smsService.sendSMS(phoneNumber, title + ": " + smsMessage);
    }

    private String buildEmailBody(String username, String title, String message, String category) {
        return String.format("""
            Dear %s,
            
            %s
            
            %s
            
            ---
            This is an automated notification from ISP Management System.
            To manage your notification preferences, log in to your account.
            
            Best regards,
            ISP Management Team
            """, username, title, message);
    }

    // Public methods for managing notifications

    public List<Notification> getUserNotifications(String userId, int limit) {
        return notificationRepo.findByUserId(userId, limit);
    }

    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepo.findUnreadByUserId(userId);
    }

    public int getUnreadCount(String userId) {
        return notificationRepo.getUnreadCount(userId);
    }

    public void markAsRead(String notificationId) {
        notificationRepo.markAsRead(notificationId);
    }

    public void markAllAsRead(String userId) {
        notificationRepo.markAllAsRead(userId);
    }

    public NotificationPreferences getPreferences(String userId) {
        return preferencesRepo.findByUserId(userId);
    }

    public void updatePreferences(NotificationPreferences preferences) {
        preferencesRepo.save(preferences);
    }

    public void cleanupOldNotifications(int daysOld) {
        notificationRepo.deleteOldNotifications(daysOld);
    }
}
