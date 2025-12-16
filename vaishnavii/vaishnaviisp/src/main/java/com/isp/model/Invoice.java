package com.isp.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Invoice {
    private String invoiceId;
    private String paymentId;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String planName;
    private double amount;
    private double taxAmount;
    private double totalAmount;
    private LocalDateTime invoiceDate;
    private String invoiceNumber;
    private String pdfPath;

    // Constructor
    public Invoice() {
        this.invoiceId = UUID.randomUUID().toString();
        this.invoiceDate = LocalDateTime.now();
        this.invoiceNumber = generateInvoiceNumber();
    }

    public Invoice(String paymentId, String customerId, String customerName, String customerEmail, 
                   String planName, double amount) {
        this();
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.planName = planName;
        this.amount = amount;
        this.taxAmount = calculateTax(amount);
        this.totalAmount = amount + taxAmount;
    }

    private String generateInvoiceNumber() {
        // Format: INV-YYYYMMDD-XXXX
        LocalDateTime now = LocalDateTime.now();
        String datePart = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String randomPart = String.format("%04d", (int)(Math.random() * 10000));
        return "INV-" + datePart + "-" + randomPart;
    }

    private double calculateTax(double amount) {
        // 18% GST
        return amount * 0.18;
    }

    // Getters and Setters
    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceId='" + invoiceId + '\'' +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", totalAmount=" + totalAmount +
                ", invoiceDate=" + invoiceDate +
                '}';
    }
}
