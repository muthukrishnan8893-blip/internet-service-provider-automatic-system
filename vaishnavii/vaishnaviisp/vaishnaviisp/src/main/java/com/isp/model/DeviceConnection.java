package com.isp.model;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Represents a device connected to a customer's hotspot.
 * Tracks device information, connection time, and data usage.
 */
public class DeviceConnection {
    private final String id;
    private final String customerId;
    private final String deviceName;
    private final String macAddress;
    private final LocalDateTime connectTime;
    private LocalDateTime disconnectTime;
    private double dataUsedGB;
    private boolean isActive;

    public DeviceConnection(String id, String customerId, String deviceName, String macAddress) {
        this.id = id;
        this.customerId = customerId;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.connectTime = LocalDateTime.now();
        this.dataUsedGB = 0.0;
        this.isActive = true;
    }

    public DeviceConnection(String id, String customerId, String deviceName, String macAddress, LocalDateTime connectTime) {
        this.id = id;
        this.customerId = customerId;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.connectTime = connectTime;
        this.dataUsedGB = 0.0;
        this.isActive = true;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    public LocalDateTime getDisconnectTime() {
        return disconnectTime;
    }

    public void setDisconnectTime(LocalDateTime disconnectTime) {
        this.disconnectTime = disconnectTime;
        this.isActive = false;
    }

    public double getDataUsedGB() {
        return dataUsedGB;
    }

    public void addDataUsage(double dataGB) {
        this.dataUsedGB += dataGB;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * Calculate connection duration in minutes.
     */
    public long getConnectionDurationMinutes() {
        LocalDateTime endTime = isActive ? LocalDateTime.now() : disconnectTime;
        return Duration.between(connectTime, endTime).toMinutes();
    }

    /**
     * Disconnect the device and record final data usage.
     */
    public void disconnect(double finalDataUsedGB) {
        this.dataUsedGB = finalDataUsedGB;
        this.disconnectTime = LocalDateTime.now();
        this.isActive = false;
    }

    @Override
    public String toString() {
        String status = isActive ? "ACTIVE" : "DISCONNECTED";
        String duration = getConnectionDurationMinutes() + " min";
        return String.format("Device{id='%s', name='%s', MAC='%s', status=%s, data=%.2f GB, duration=%s}",
                id, deviceName, macAddress, status, dataUsedGB, duration);
    }
}
