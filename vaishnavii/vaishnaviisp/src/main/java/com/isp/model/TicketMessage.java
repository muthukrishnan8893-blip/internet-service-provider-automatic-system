package com.isp.model;

import java.time.LocalDateTime;

/**
 * Represents a message/chat in a support ticket.
 */
public class TicketMessage {
    private final String id;
    private final String ticketId;
    private final String senderId; // User ID
    private String senderName;
    private String message;
    private LocalDateTime sentAt;
    private String type; // USER, ADMIN, SYSTEM

    public TicketMessage(String id, String ticketId, String senderId, String senderName, String message, String type) {
        this.id = id;
        this.ticketId = ticketId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.type = type;
        this.sentAt = LocalDateTime.now();
    }

    // Getters
    public String getId() { return id; }
    public String getTicketId() { return ticketId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return "TicketMessage{" +
                "id='" + id + '\'' +
                ", senderName='" + senderName + '\'' +
                ", type='" + type + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}
