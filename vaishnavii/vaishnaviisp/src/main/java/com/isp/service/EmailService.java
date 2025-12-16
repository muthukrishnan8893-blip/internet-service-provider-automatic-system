package com.isp.service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Email notification service using Gmail SMTP.
 */
public class EmailService {
    private boolean emailEnabled;
    private String smtpHost;
    private int smtpPort;
    private String fromEmail;
    private String smtpUsername;
    private String smtpPassword;

    public EmailService() {
        // Gmail SMTP configuration
        this.emailEnabled = true;
        this.fromEmail = "muthuvel04041971@gmail.com";
        this.smtpUsername = "muthuvel04041971@gmail.com";
        // App-specific password for Gmail (must be configured)
        this.smtpPassword = System.getenv("GMAIL_APP_PASSWORD");
        this.smtpHost = "smtp.gmail.com";
        this.smtpPort = 587;
        
        if (smtpPassword == null || smtpPassword.isEmpty()) {
            System.out.println("[EMAIL] Warning: GMAIL_APP_PASSWORD not set. Email sending will be simulated.");
            System.out.println("[EMAIL] To enable real email: Set GMAIL_APP_PASSWORD environment variable with Gmail app password");
            this.emailEnabled = false;
        }
    }

    /**
     * Send email notification using Gmail SMTP.
     */
    public void sendEmail(String toEmail, String subject, String body) {
        if (!emailEnabled) {
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║    EMAIL NOTIFICATION (SIMULATED)     ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ To: " + padRight(toEmail, 32) + "║");
            System.out.println("║ From: " + padRight(fromEmail, 30) + "║");
            System.out.println("║ Subject: " + padRight(subject, 27) + "║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ " + padRight(body, 38) + "║");
            System.out.println("╚════════════════════════════════════════╝");
            return;
        }

        try {
            // Configure Gmail SMTP properties
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.ssl.trust", smtpHost);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            // Send email
            Transport.send(message);
            System.out.println("[EMAIL] ✓ Sent to: " + toEmail + " | Subject: " + subject);
            
        } catch (Exception e) {
            System.err.println("[EMAIL] Failed to send email: " + e.getMessage());
            // Fallback to console logging
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║    EMAIL NOTIFICATION (FAILED)        ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ To: " + padRight(toEmail, 32) + "║");
            System.out.println("║ From: " + padRight(fromEmail, 30) + "║");
            System.out.println("║ Subject: " + padRight(subject, 27) + "║");
            System.out.println("╚════════════════════════════════════════╝");
        }
    }

    /**
     * Send notification email on customer registration.
     */
    public void sendRegistrationEmail(String email, String username, String planName) {
        String subject = "Registration Confirmed - ISP Management";
        String body = "Dear " + username + ",\n\n" +
                "Your account has been successfully created.\n" +
                "Plan: " + planName + "\n\n" +
                "Thank you for choosing our service!";
        sendEmail(email, subject, body);
    }

    /**
     * Send notification email on plan selection.
     */
    public void sendPlanConfirmationEmail(String email, String customerName, String planName, double monthlyPrice) {
        String subject = "Plan Confirmation - " + planName;
        String body = "Dear " + customerName + ",\n\n" +
                "Your plan has been updated to " + planName + "\n" +
                "Monthly charge: $" + monthlyPrice + "\n\n" +
                "Plan details and billing will be sent separately.";
        sendEmail(email, subject, body);
    }

    /**
     * Send notification email when ticket is created.
     */
    public void sendTicketCreatedEmail(String email, String customerName, String ticketId, String subject) {
        String emailBody = "Dear " + customerName + ",\n\n" +
                "Your support ticket has been created.\n" +
                "Ticket ID: " + ticketId + "\n" +
                "Subject: " + subject + "\n\n" +
                "An admin will respond shortly. You will receive updates via email.";
        sendEmail(email, "Support Ticket Created - " + ticketId, emailBody);
    }

    /**
     * Send notification email when admin responds to ticket.
     */
    public void sendAdminReplyEmail(String email, String customerName, String ticketId, String adminMessage) {
        String emailBody = "Dear " + customerName + ",\n\n" +
                "An admin has replied to your ticket #" + ticketId + ":\n\n" +
                "---\n" + adminMessage + "\n---\n\n" +
                "Please log in to your dashboard to view the full conversation.";
        sendEmail(email, "New Reply on Ticket #" + ticketId, emailBody);
    }

    /**
     * Send monthly billing notification.
     */
    public void sendBillingEmail(String email, String customerName, double amount, String invoiceId) {
        String subject = "Monthly Invoice - " + invoiceId;
        String body = "Dear " + customerName + ",\n\n" +
                "Your monthly invoice is ready.\n" +
                "Invoice ID: " + invoiceId + "\n" +
                "Amount Due: $" + amount + "\n\n" +
                "Please pay within 7 days. Download your invoice from your dashboard.";
        sendEmail(email, subject, body);
    }

    private String padRight(String s, int length) {
        return String.format("%-" + length + "s", s).substring(0, Math.min(s.length(), length));
    }
}
