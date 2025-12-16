package com.isp.model;

import java.time.LocalDateTime;

/**
 * Model for daily usage tracking
 */
public class DailyUsage {
    private String id;
    private String customerId;
    private LocalDateTime date;
    private double dataUsedGB;
    private double uploadGB;
    private double downloadGB;
    private double peakSpeedMbps;
    private int totalDevicesConnected;
    
    public DailyUsage() {}
    
    public DailyUsage(String id, String customerId, LocalDateTime date, double dataUsedGB,
                     double uploadGB, double downloadGB, double peakSpeedMbps, int totalDevicesConnected) {
        this.id = id;
        this.customerId = customerId;
        this.date = date;
        this.dataUsedGB = dataUsedGB;
        this.uploadGB = uploadGB;
        this.downloadGB = downloadGB;
        this.peakSpeedMbps = peakSpeedMbps;
        this.totalDevicesConnected = totalDevicesConnected;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public double getDataUsedGB() { return dataUsedGB; }
    public void setDataUsedGB(double dataUsedGB) { this.dataUsedGB = dataUsedGB; }
    
    public double getUploadGB() { return uploadGB; }
    public void setUploadGB(double uploadGB) { this.uploadGB = uploadGB; }
    
    public double getDownloadGB() { return downloadGB; }
    public void setDownloadGB(double downloadGB) { this.downloadGB = downloadGB; }
    
    public double getPeakSpeedMbps() { return peakSpeedMbps; }
    public void setPeakSpeedMbps(double peakSpeedMbps) { this.peakSpeedMbps = peakSpeedMbps; }
    
    public int getTotalDevicesConnected() { return totalDevicesConnected; }
    public void setTotalDevicesConnected(int totalDevicesConnected) { this.totalDevicesConnected = totalDevicesConnected; }
}
