package com.isp.repo;

import com.isp.model.TicketEnhanced;
import com.isp.model.TicketMessage;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for enhanced tickets with database persistence.
 */
public class TicketEnhancedRepository {

    public void save(TicketEnhanced ticket) {
        String ticketSql = """
            INSERT INTO tickets (id, customer_id, customer_name, subject, description, status, priority, created_at, resolved_at, assigned_to_admin_id, assigned_to_admin_name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                customer_id = VALUES(customer_id),
                customer_name = VALUES(customer_name),
                subject = VALUES(subject),
                description = VALUES(description),
                status = VALUES(status),
                priority = VALUES(priority),
                resolved_at = VALUES(resolved_at),
                assigned_to_admin_id = VALUES(assigned_to_admin_id),
                assigned_to_admin_name = VALUES(assigned_to_admin_name)
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ticketSql)) {
            
            stmt.setString(1, ticket.getId());
            stmt.setString(2, ticket.getCustomerId());
            stmt.setString(3, ticket.getCustomerName());
            stmt.setString(4, ticket.getSubject());
            stmt.setString(5, ticket.getDescription());
            stmt.setString(6, ticket.getStatus().name());
            stmt.setString(7, ticket.getPriority().name());
            stmt.setTimestamp(8, Timestamp.valueOf(ticket.getCreatedAt()));
            stmt.setTimestamp(9, ticket.getResolvedAt() != null ? Timestamp.valueOf(ticket.getResolvedAt()) : null);
            stmt.setString(10, ticket.getAssignedToAdminId());
            stmt.setString(11, ticket.getAssignedToAdminName());
            
            stmt.executeUpdate();
            
            // Save messages
            saveMessages(ticket.getId(), ticket.getMessages());
            
        } catch (SQLException e) {
            System.err.println("[TicketEnhancedRepository] Error saving ticket: " + e.getMessage());
            throw new RuntimeException("Failed to save ticket", e);
        }
    }
    
    private void saveMessages(String ticketId, List<TicketMessage> messages) throws SQLException {
        String deleteSql = "DELETE FROM ticket_messages WHERE ticket_id = ?";
        String insertSql = """
            INSERT INTO ticket_messages (id, ticket_id, sender_id, sender_name, message, message_type, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            
            // Delete existing messages
            deleteStmt.setString(1, ticketId);
            deleteStmt.executeUpdate();
            
            // Insert all messages
            for (TicketMessage msg : messages) {
                insertStmt.setString(1, msg.getId());
                insertStmt.setString(2, msg.getTicketId());
                insertStmt.setString(3, msg.getSenderId());
                insertStmt.setString(4, msg.getSenderName());
                insertStmt.setString(5, msg.getMessage());
                insertStmt.setString(6, msg.getType());
                insertStmt.setTimestamp(7, Timestamp.valueOf(msg.getSentAt()));
                insertStmt.executeUpdate();
            }
        }
    }

    public Optional<TicketEnhanced> findById(String id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        TicketEnhanced ticket = null;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                ticket = mapResultSetToTicket(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("[TicketEnhancedRepository] Error finding ticket by id: " + e.getMessage());
        }
        
        if (ticket != null) {
            try {
                loadMessages(ticket);
            } catch (SQLException e) {
                System.err.println("[TicketEnhancedRepository] Error loading messages: " + e.getMessage());
            }
            return Optional.of(ticket);
        }
        
        return Optional.empty();
    }

    public List<TicketEnhanced> findByCustomerId(String customerId) {
        String sql = "SELECT * FROM tickets WHERE customer_id = ? ORDER BY created_at DESC";
        List<TicketEnhanced> tickets = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[TicketEnhancedRepository] Error finding tickets by customer: " + e.getMessage());
        }
        
        // Load messages for all tickets after closing the main ResultSet
        for (TicketEnhanced ticket : tickets) {
            try {
                loadMessages(ticket);
            } catch (SQLException e) {
                System.err.println("[TicketEnhancedRepository] Error loading messages for ticket: " + e.getMessage());
            }
        }
        
        return tickets;
    }

    public List<TicketEnhanced> findByAdminId(String adminId) {
        String sql = "SELECT * FROM tickets WHERE assigned_to_admin_id = ? ORDER BY created_at DESC";
        List<TicketEnhanced> tickets = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, adminId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[TicketEnhancedRepository] Error finding tickets by admin: " + e.getMessage());
        }
        
        // Load messages for all tickets after closing the main ResultSet
        for (TicketEnhanced ticket : tickets) {
            try {
                loadMessages(ticket);
            } catch (SQLException e) {
                System.err.println("[TicketEnhancedRepository] Error loading messages for ticket: " + e.getMessage());
            }
        }
        
        return tickets;
    }

    public List<TicketEnhanced> findAll() {
        String sql = "SELECT * FROM tickets ORDER BY created_at DESC";
        List<TicketEnhanced> tickets = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[TicketEnhancedRepository] Error finding all tickets: " + e.getMessage());
        }
        
        // Load messages for all tickets after closing the main ResultSet
        for (TicketEnhanced ticket : tickets) {
            try {
                loadMessages(ticket);
            } catch (SQLException e) {
                System.err.println("[TicketEnhancedRepository] Error loading messages for ticket: " + e.getMessage());
            }
        }
        
        return tickets;
    }
    
    private TicketEnhanced mapResultSetToTicket(ResultSet rs) throws SQLException {
        TicketEnhanced ticket = new TicketEnhanced(
            rs.getString("customer_id"),
            rs.getString("customer_name"),
            rs.getString("subject"),
            rs.getString("description")
        );
        
        // Set ID using reflection to bypass constructor
        try {
            java.lang.reflect.Field idField = TicketEnhanced.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ticket, rs.getString("id"));
            
            java.lang.reflect.Field createdAtField = TicketEnhanced.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(ticket, rs.getTimestamp("created_at").toLocalDateTime());
        } catch (Exception e) {
            throw new SQLException("Failed to set ticket fields", e);
        }
        
        ticket.setStatus(TicketEnhanced.Status.valueOf(rs.getString("status")));
        ticket.setPriority(TicketEnhanced.Priority.valueOf(rs.getString("priority")));
        
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) {
            try {
                java.lang.reflect.Field resolvedAtField = TicketEnhanced.class.getDeclaredField("resolvedAt");
                resolvedAtField.setAccessible(true);
                resolvedAtField.set(ticket, resolvedAt.toLocalDateTime());
            } catch (Exception ignored) {}
        }
        
        ticket.setAssignedToAdminId(rs.getString("assigned_to_admin_id"));
        ticket.setAssignedToAdminName(rs.getString("assigned_to_admin_name"));
        
        return ticket;
    }
    
    private void loadMessages(TicketEnhanced ticket) throws SQLException {
        String sql = "SELECT * FROM ticket_messages WHERE ticket_id = ? ORDER BY created_at ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ticket.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                TicketMessage msg = new TicketMessage(
                    rs.getString("id"),
                    rs.getString("ticket_id"),
                    rs.getString("sender_id"),
                    rs.getString("sender_name"),
                    rs.getString("message"),
                    rs.getString("message_type")
                );
                
                // Set sentAt using reflection
                try {
                    java.lang.reflect.Field sentAtField = TicketMessage.class.getDeclaredField("sentAt");
                    sentAtField.setAccessible(true);
                    sentAtField.set(msg, rs.getTimestamp("created_at").toLocalDateTime());
                } catch (Exception ignored) {}
                
                ticket.addMessage(msg);
            }
        }
    }
}
