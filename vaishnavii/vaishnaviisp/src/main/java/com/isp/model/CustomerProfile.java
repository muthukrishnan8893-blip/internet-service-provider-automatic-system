package com.isp.model;

import java.time.LocalDateTime;

/**
 * Extended customer profile linked to a user.
 */
public class CustomerProfile {
    private final String id;
    private final String userId;
    private String fullName;
    private String phoneNumber;
    private String address;
    private DataPlan currentPlan;
    private LocalDateTime planStartDate;
    private LocalDateTime planRenewalDate;
    private double accountBalance;
    private boolean isActive;

    public CustomerProfile(String id, String userId, String fullName) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.accountBalance = 0.0;
        this.isActive = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public DataPlan getCurrentPlan() { return currentPlan; }
    public void setCurrentPlan(DataPlan currentPlan) { this.currentPlan = currentPlan; }
    public LocalDateTime getPlanStartDate() { return planStartDate; }
    public void setPlanStartDate(LocalDateTime planStartDate) { this.planStartDate = planStartDate; }
    public LocalDateTime getPlanRenewalDate() { return planRenewalDate; }
    public void setPlanRenewalDate(LocalDateTime planRenewalDate) { this.planRenewalDate = planRenewalDate; }
    public double getAccountBalance() { return accountBalance; }
    public void setAccountBalance(double accountBalance) { this.accountBalance = accountBalance; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "CustomerProfile{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", currentPlan=" + (currentPlan != null ? currentPlan.getName() : "None") +
                ", isActive=" + isActive +
                '}';
    }
}
