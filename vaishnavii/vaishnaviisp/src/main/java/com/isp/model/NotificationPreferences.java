package com.isp.model;

/**
 * User notification preferences
 */
public class NotificationPreferences {
    private String userId;
    
    // Email preferences
    private boolean emailEnabled;
    private boolean emailUsageAlerts;
    private boolean emailPaymentReminders;
    private boolean emailTicketUpdates;
    private boolean emailSecurityAlerts;
    private boolean emailPromotions;
    
    // Browser push preferences
    private boolean browserEnabled;
    private boolean browserUsageAlerts;
    private boolean browserPaymentReminders;
    private boolean browserTicketUpdates;
    private boolean browserSecurityAlerts;
    
    // SMS preferences
    private boolean smsEnabled;
    private boolean smsCriticalOnly;
    private boolean smsUsageAlerts;
    private boolean smsPaymentReminders;
    private boolean smsSecurityAlerts;
    private String phoneNumber;
    
    // Usage alert thresholds
    private int usageAlertThreshold1; // e.g., 50%
    private int usageAlertThreshold2; // e.g., 75%
    private int usageAlertThreshold3; // e.g., 90%

    public NotificationPreferences() {
        // Default preferences - all enabled
        this.emailEnabled = true;
        this.emailUsageAlerts = true;
        this.emailPaymentReminders = true;
        this.emailTicketUpdates = true;
        this.emailSecurityAlerts = true;
        this.emailPromotions = false;
        
        this.browserEnabled = true;
        this.browserUsageAlerts = true;
        this.browserPaymentReminders = true;
        this.browserTicketUpdates = true;
        this.browserSecurityAlerts = true;
        
        this.smsEnabled = false;
        this.smsCriticalOnly = true;
        this.smsUsageAlerts = false;
        this.smsPaymentReminders = false;
        this.smsSecurityAlerts = true;
        
        this.usageAlertThreshold1 = 50;
        this.usageAlertThreshold2 = 75;
        this.usageAlertThreshold3 = 90;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isEmailUsageAlerts() {
        return emailUsageAlerts;
    }

    public void setEmailUsageAlerts(boolean emailUsageAlerts) {
        this.emailUsageAlerts = emailUsageAlerts;
    }

    public boolean isEmailPaymentReminders() {
        return emailPaymentReminders;
    }

    public void setEmailPaymentReminders(boolean emailPaymentReminders) {
        this.emailPaymentReminders = emailPaymentReminders;
    }

    public boolean isEmailTicketUpdates() {
        return emailTicketUpdates;
    }

    public void setEmailTicketUpdates(boolean emailTicketUpdates) {
        this.emailTicketUpdates = emailTicketUpdates;
    }

    public boolean isEmailSecurityAlerts() {
        return emailSecurityAlerts;
    }

    public void setEmailSecurityAlerts(boolean emailSecurityAlerts) {
        this.emailSecurityAlerts = emailSecurityAlerts;
    }

    public boolean isEmailPromotions() {
        return emailPromotions;
    }

    public void setEmailPromotions(boolean emailPromotions) {
        this.emailPromotions = emailPromotions;
    }

    public boolean isBrowserEnabled() {
        return browserEnabled;
    }

    public void setBrowserEnabled(boolean browserEnabled) {
        this.browserEnabled = browserEnabled;
    }

    public boolean isBrowserUsageAlerts() {
        return browserUsageAlerts;
    }

    public void setBrowserUsageAlerts(boolean browserUsageAlerts) {
        this.browserUsageAlerts = browserUsageAlerts;
    }

    public boolean isBrowserPaymentReminders() {
        return browserPaymentReminders;
    }

    public void setBrowserPaymentReminders(boolean browserPaymentReminders) {
        this.browserPaymentReminders = browserPaymentReminders;
    }

    public boolean isBrowserTicketUpdates() {
        return browserTicketUpdates;
    }

    public void setBrowserTicketUpdates(boolean browserTicketUpdates) {
        this.browserTicketUpdates = browserTicketUpdates;
    }

    public boolean isBrowserSecurityAlerts() {
        return browserSecurityAlerts;
    }

    public void setBrowserSecurityAlerts(boolean browserSecurityAlerts) {
        this.browserSecurityAlerts = browserSecurityAlerts;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public boolean isSmsCriticalOnly() {
        return smsCriticalOnly;
    }

    public void setSmsCriticalOnly(boolean smsCriticalOnly) {
        this.smsCriticalOnly = smsCriticalOnly;
    }

    public boolean isSmsUsageAlerts() {
        return smsUsageAlerts;
    }

    public void setSmsUsageAlerts(boolean smsUsageAlerts) {
        this.smsUsageAlerts = smsUsageAlerts;
    }

    public boolean isSmsPaymentReminders() {
        return smsPaymentReminders;
    }

    public void setSmsPaymentReminders(boolean smsPaymentReminders) {
        this.smsPaymentReminders = smsPaymentReminders;
    }

    public boolean isSmsSecurityAlerts() {
        return smsSecurityAlerts;
    }

    public void setSmsSecurityAlerts(boolean smsSecurityAlerts) {
        this.smsSecurityAlerts = smsSecurityAlerts;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getUsageAlertThreshold1() {
        return usageAlertThreshold1;
    }

    public void setUsageAlertThreshold1(int usageAlertThreshold1) {
        this.usageAlertThreshold1 = usageAlertThreshold1;
    }

    public int getUsageAlertThreshold2() {
        return usageAlertThreshold2;
    }

    public void setUsageAlertThreshold2(int usageAlertThreshold2) {
        this.usageAlertThreshold2 = usageAlertThreshold2;
    }

    public int getUsageAlertThreshold3() {
        return usageAlertThreshold3;
    }

    public void setUsageAlertThreshold3(int usageAlertThreshold3) {
        this.usageAlertThreshold3 = usageAlertThreshold3;
    }
}
