package com.isp.model;

import java.time.LocalDateTime;

/**
 * Represents network usage data for a customer.
 */
public class NetworkUsage {
    private final String id;
    private final String customerId;
    private final double gigabytes;
    private final LocalDateTime timestamp;

    public NetworkUsage(String id, String customerId, double gigabytes, LocalDateTime timestamp) {
        this.id = id;
        this.customerId = customerId;
        this.gigabytes = gigabytes;
        this.timestamp = timestamp;
    }

    public String getId() { 
        return id; 
    }
    
    public String getCustomerId() { 
        return customerId; 
    }
    
    public double getGigabytes() { 
        return gigabytes; 
    }
    
    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }

    @Override
    public String toString() {
        return "NetworkUsage{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", gb=" + gigabytes +
                ", timestamp=" + timestamp +
                '}';
    }
}
