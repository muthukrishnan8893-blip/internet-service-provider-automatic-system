package com.isp.model;

import java.time.LocalDateTime;

/**
 * Model for speed test results
 */
public class SpeedTest {
    private String id;
    private String customerId;
    private LocalDateTime testTime;
    private double downloadSpeedMbps;
    private double uploadSpeedMbps;
    private int pingMs;
    
    public SpeedTest() {}
    
    public SpeedTest(String id, String customerId, LocalDateTime testTime, 
                    double downloadSpeedMbps, double uploadSpeedMbps, int pingMs) {
        this.id = id;
        this.customerId = customerId;
        this.testTime = testTime;
        this.downloadSpeedMbps = downloadSpeedMbps;
        this.uploadSpeedMbps = uploadSpeedMbps;
        this.pingMs = pingMs;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public LocalDateTime getTestTime() { return testTime; }
    public void setTestTime(LocalDateTime testTime) { this.testTime = testTime; }
    
    public double getDownloadSpeedMbps() { return downloadSpeedMbps; }
    public void setDownloadSpeedMbps(double downloadSpeedMbps) { this.downloadSpeedMbps = downloadSpeedMbps; }
    
    public double getUploadSpeedMbps() { return uploadSpeedMbps; }
    public void setUploadSpeedMbps(double uploadSpeedMbps) { this.uploadSpeedMbps = uploadSpeedMbps; }
    
    public int getPingMs() { return pingMs; }
    public void setPingMs(int pingMs) { this.pingMs = pingMs; }
    
    // Helper method to determine connection quality
    public String getQuality() {
        if (downloadSpeedMbps >= 50 && uploadSpeedMbps >= 10 && pingMs < 30) {
            return "Excellent";
        } else if (downloadSpeedMbps >= 30 && uploadSpeedMbps >= 5 && pingMs < 50) {
            return "Good";
        } else if (downloadSpeedMbps >= 10 && uploadSpeedMbps >= 2 && pingMs < 100) {
            return "Fair";
        } else {
            return "Poor";
        }
    }
}
