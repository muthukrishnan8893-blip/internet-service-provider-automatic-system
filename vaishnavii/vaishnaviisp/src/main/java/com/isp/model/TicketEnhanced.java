package com.isp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced ticket with messaging and assignment.
 */
public class TicketEnhanced {
    public enum Status {
        OPEN, IN_PROGRESS, RESOLVED, CLOSED, WAITING_CUSTOMER
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    private final String id;
    private final String customerId;
    private final String customerName;
    private String subject;
    private String description;
    private Status status;
    private Priority priority;
    private final LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String assignedToAdminId;
    private String assignedToAdminName;
    private List<TicketMessage> messages;

    public TicketEnhanced(String customerId, String customerName, String subject, String description) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.customerName = customerName;
        this.subject = subject;
        this.description = description;
        this.status = Status.OPEN;
        this.priority = Priority.MEDIUM;
        this.createdAt = LocalDateTime.now();
        this.messages = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.RESOLVED || status == Status.CLOSED) {
            this.resolvedAt = LocalDateTime.now();
        }
    }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getAssignedToAdminId() { return assignedToAdminId; }
    public void setAssignedToAdminId(String assignedToAdminId) { this.assignedToAdminId = assignedToAdminId; }
    public String getAssignedToAdminName() { return assignedToAdminName; }
    public void setAssignedToAdminName(String assignedToAdminName) { this.assignedToAdminName = assignedToAdminName; }
    public List<TicketMessage> getMessages() { return messages; }
    public void addMessage(TicketMessage message) { this.messages.add(message); }

    @Override
    public String toString() {
        return "TicketEnhanced{" +
                "id='" + id + '\'' +
                ", subject='" + subject + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", messages=" + messages.size() +
                '}';
    }
}
