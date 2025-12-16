package com.isp.service;

/**
 * SMS notification service (simulated)
 * In production, integrate with Twilio, AWS SNS, or other SMS provider
 */
public class SmsService {
    private boolean smsEnabled;
    private String apiKey;
    private String senderId;

    public SmsService() {
        this.apiKey = System.getenv("SMS_API_KEY");
        this.senderId = "ISP-MGT";
        this.smsEnabled = (apiKey != null && !apiKey.isEmpty());
        
        if (!smsEnabled) {
            System.out.println("[SMS] Warning: SMS_API_KEY not set. SMS sending will be simulated.");
            System.out.println("[SMS] To enable real SMS: Set SMS_API_KEY environment variable");
        }
    }

    /**
     * Send SMS notification
     */
    public void sendSMS(String phoneNumber, String message) {
        if (!smsEnabled) {
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║      SMS NOTIFICATION (SIMULATED)     ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ To: " + padRight(phoneNumber, 33) + "║");
            System.out.println("║ From: " + padRight(senderId, 31) + "║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ " + padRight(message, 38) + "║");
            System.out.println("╚════════════════════════════════════════╝");
            return;
        }

        try {
            // In production, call SMS API here
            // Example for Twilio:
            // Message.creator(
            //     new PhoneNumber(phoneNumber),
            //     new PhoneNumber(senderNumber),
            //     message
            // ).create();
            
            System.out.println("[SMS] ✓ Sent to: " + phoneNumber + " | Message: " + message.substring(0, Math.min(50, message.length())) + "...");
            
        } catch (Exception e) {
            System.err.println("[SMS] Failed to send SMS: " + e.getMessage());
            // Fallback to console logging
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║      SMS NOTIFICATION (FAILED)        ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ To: " + padRight(phoneNumber, 33) + "║");
            System.out.println("╚════════════════════════════════════════╝");
        }
    }

    /**
     * Send critical alert SMS
     */
    public void sendCriticalAlert(String phoneNumber, String title, String message) {
        String smsMessage = "⚠️ CRITICAL ALERT: " + title + "\n" + message;
        sendSMS(phoneNumber, smsMessage);
    }

    /**
     * Send usage alert SMS
     */
    public void sendUsageAlert(String phoneNumber, String username, int usagePercent, double remainingGB) {
        String message = String.format(
            "Hi %s, you've used %d%% of your data plan. %.2f GB remaining. Manage your usage at isp.example.com",
            username, usagePercent, remainingGB
        );
        sendSMS(phoneNumber, message);
    }

    /**
     * Send payment reminder SMS
     */
    public void sendPaymentReminder(String phoneNumber, String username, double amount, String dueDate) {
        String message = String.format(
            "Hi %s, payment of $%.2f is due on %s. Pay now to avoid service interruption.",
            username, amount, dueDate
        );
        sendSMS(phoneNumber, message);
    }

    /**
     * Send OTP SMS
     */
    public void sendOTP(String phoneNumber, String otp) {
        String message = String.format(
            "Your ISP Management verification code is: %s. Valid for 10 minutes. Do not share this code.",
            otp
        );
        sendSMS(phoneNumber, message);
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() > n) return s.substring(0, n);
        return String.format("%-" + n + "s", s);
    }
}
