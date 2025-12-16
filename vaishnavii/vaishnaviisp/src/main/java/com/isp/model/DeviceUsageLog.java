package com.isp.model;

import java.time.LocalDateTime;

/**
 * Detailed log of device data usage at specific time intervals.
 */
public class DeviceUsageLog {
    private final String id;
    private final String deviceConnectionId;
    private final String customerId;
    private double dataUsedGB;
    private LocalDateTime timestamp;
    private String status; // ACTIVE, IDLE, DISCONNECTED

    public DeviceUsageLog(String id, String deviceConnectionId, String customerId, double dataUsedGB) {
        this.id = id;
        this.deviceConnectionId = deviceConnectionId;
        this.customerId = customerId;
        this.dataUsedGB = dataUsedGB;
        this.timestamp = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getDeviceConnectionId() { return deviceConnectionId; }
    public String getCustomerId() { return customerId; }
    public double getDataUsedGB() { return dataUsedGB; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "DeviceUsageLog{" +
                "id='" + id + '\'' +
                ", dataUsedGB=" + dataUsedGB +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}
