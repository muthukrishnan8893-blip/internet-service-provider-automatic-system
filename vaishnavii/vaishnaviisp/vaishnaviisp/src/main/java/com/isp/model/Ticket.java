package com.isp.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a support ticket for troubleshooting.
 */
public class Ticket {
    public enum Status { 
        OPEN, 
        IN_PROGRESS, 
        RESOLVED, 
        CLOSED 
    }

    private final String id;
    private final String customerId;
    private final String description;
    private Status status;
    private final LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public Ticket(String customerId, String description) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.description = description;
        this.status = Status.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { 
        return id; 
    }
    
    public String getCustomerId() { 
        return customerId; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public Status getStatus() { 
        return status; 
    }
    
    public void setStatus(Status status) { 
        this.status = status;
        if (status == Status.RESOLVED || status == Status.CLOSED) {
            this.resolvedAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }
    
    public LocalDateTime getResolvedAt() { 
        return resolvedAt; 
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", status=" + status +
                ", description='" + description + '\'' +
                '}';
    }
}
