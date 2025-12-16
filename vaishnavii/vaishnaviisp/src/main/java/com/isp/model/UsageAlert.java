package com.isp.model;

import java.time.LocalDateTime;

/**
 * Model for bandwidth usage alerts
 */
public class UsageAlert {
    public enum AlertType {
        WARNING_80,    // 80% usage
        WARNING_90,    // 90% usage
        LIMIT_REACHED  // 100% usage
    }
    
    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        RESOLVED
    }
    
    private String id;
    private String customerId;
    private AlertType alertType;
    private AlertStatus status;
    private double usagePercentage;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    
    public UsageAlert() {}
    
    public UsageAlert(String id, String customerId, AlertType alertType, AlertStatus status, 
                     double usagePercentage, LocalDateTime createdAt, LocalDateTime acknowledgedAt) {
        this.id = id;
        this.customerId = customerId;
        this.alertType = alertType;
        this.status = status;
        this.usagePercentage = usagePercentage;
        this.createdAt = createdAt;
        this.acknowledgedAt = acknowledgedAt;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    
    public double getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(double usagePercentage) { this.usagePercentage = usagePercentage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    
    // Helper method to get formatted message
    public String getMessage() {
        switch (alertType) {
            case WARNING_80:
                return String.format("Warning: You have used %.1f%% of your data plan!", usagePercentage);
            case WARNING_90:
                return String.format("High Usage Alert: You have used %.1f%% of your data plan!", usagePercentage);
            case LIMIT_REACHED:
                return String.format("Data Limit Reached: You have used %.1f%% of your data plan!", usagePercentage);
            default:
                return "Usage alert";
        }
    }
}
