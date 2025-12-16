package com.isp.service;

import com.isp.model.TicketEnhanced;
import com.isp.model.TicketMessage;
import com.isp.repo.TicketEnhancedRepository;
import com.isp.util.IdGenerator;

import java.util.List;
import java.util.Optional;

/**
 * Service for enhanced ticket management with messaging.
 */
public class TicketEnhancedService {
    private final TicketEnhancedRepository repository;
    private final EmailService emailService;

    public TicketEnhancedService(TicketEnhancedRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    /**
     * Create a new support ticket and notify admin.
     */
    public TicketEnhanced createTicket(String customerId, String customerName, String subject, String description, String email) {
        TicketEnhanced ticket = new TicketEnhanced(customerId, customerName, subject, description);
        repository.save(ticket);

        // Send notification to customer
        emailService.sendTicketCreatedEmail(email, customerName, ticket.getId(), subject);
        
        // Send notification to admin
        String adminSubject = "New Support Ticket Created: #" + ticket.getId();
        String adminBody = "A new support ticket has been submitted:\\n\\n" +
                          "Customer: " + customerName + "\\n" +
                          "Customer Email: " + email + "\\n" +
                          "Ticket ID: " + ticket.getId() + "\\n" +
                          "Subject: " + subject + "\\n" +
                          "Description: " + description + "\\n\\n" +
                          "Please log in to the admin dashboard to respond.";
        emailService.sendEmail("muthuvel04041971@gmail.com", adminSubject, adminBody);

        System.out.println("Ticket created: " + ticket);
        return ticket;
    }

    /**
     * Add a message to a ticket and send email notification.
     */
    public void addMessage(String ticketId, String senderId, String senderName, String message, String type, String senderEmail, String recipientEmail) {
        Optional<TicketEnhanced> ticketOpt = repository.findById(ticketId);
        if (ticketOpt.isPresent()) {
            TicketEnhanced ticket = ticketOpt.get();
            TicketMessage msg = new TicketMessage(
                    IdGenerator.generate(),
                    ticketId,
                    senderId,
                    senderName,
                    message,
                    type
            );
            ticket.addMessage(msg);
            
            // If admin is responding, mark ticket as accepted and send email
            if ("ADMIN".equals(type)) {
                ticket.setStatus(TicketEnhanced.Status.IN_PROGRESS);
                // Send email from admin to customer
                String subject = "Response to your ticket #" + ticketId;
                String body = "Dear " + ticket.getCustomerName() + ",\\n\\n" +
                             "The admin has responded to your support ticket:\\n\\n" +
                             "Admin Response: " + message + "\\n\\n" +
                             "Your ticket has been marked as 'In Progress'.\\n\\n" +
                             "Best regards,\\n" +
                             "ISP Support Team\\n" +
                             "muthuvel04041971@gmail.com";
                emailService.sendEmail(recipientEmail, subject, body);
            } else {
                // Customer message - send notification to admin
                String subject = "New message on ticket #" + ticketId;
                String body = "A customer has added a message to ticket #" + ticketId + ":\\n\\n" +
                             "Customer: " + senderName + " (" + senderEmail + ")\\n" +
                             "Message: " + message + "\\n\\n" +
                             "Please log in to respond.";
                emailService.sendEmail("muthuvel04041971@gmail.com", subject, body);
            }
            
            System.out.println("Message added to ticket: " + ticketId);
        }
    }

    /**
     * Assign ticket to an admin.
     */
    public void assignToAdmin(String ticketId, String adminId, String adminName) {
        Optional<TicketEnhanced> ticketOpt = repository.findById(ticketId);
        if (ticketOpt.isPresent()) {
            TicketEnhanced ticket = ticketOpt.get();
            ticket.setAssignedToAdminId(adminId);
            ticket.setAssignedToAdminName(adminName);
            ticket.setStatus(TicketEnhanced.Status.IN_PROGRESS);
            System.out.println("Ticket assigned to admin: " + adminName);
        }
    }

    /**
     * Update ticket status.
     */
    public void updateStatus(String ticketId, TicketEnhanced.Status status) {
        Optional<TicketEnhanced> ticketOpt = repository.findById(ticketId);
        if (ticketOpt.isPresent()) {
            TicketEnhanced ticket = ticketOpt.get();
            ticket.setStatus(status);
            System.out.println("Ticket status updated: " + status);
        }
    }

    public Optional<TicketEnhanced> findById(String id) {
        return repository.findById(id);
    }

    public List<TicketEnhanced> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    public List<TicketEnhanced> findByAdminId(String adminId) {
        return repository.findByAdminId(adminId);
    }

    public List<TicketEnhanced> listAll() {
        return repository.findAll();
    }
}
