package com.isp.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification model for storing user notifications
 */
public class Notification {
    private String id;
    private String userId;
    private String type; // EMAIL, BROWSER, SMS, SYSTEM
    private String category; // USAGE_ALERT, PAYMENT, TICKET, SECURITY, SYSTEM
    private String title;
    private String message;
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    private boolean read;
    private boolean sent;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private String metadata; // JSON string for additional data

    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.read = false;
        this.sent = false;
    }

    public Notification(String userId, String type, String category, String title, String message, String priority) {
        this();
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.title = title;
        this.message = message;
        this.priority = priority;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
        if (read && this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
        if (sent && this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
