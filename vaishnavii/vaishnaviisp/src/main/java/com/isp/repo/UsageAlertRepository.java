package com.isp.repo;

import com.isp.model.UsageAlert;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for UsageAlert persistence in MySQL
 */
public class UsageAlertRepository {
    
    public UsageAlert save(UsageAlert alert) {
        String sql = "INSERT INTO usage_alerts (id, customer_id, alert_type, status, usage_percentage, created_at, acknowledged_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, alert.getId());
            stmt.setString(2, alert.getCustomerId());
            stmt.setString(3, alert.getAlertType().name());
            stmt.setString(4, alert.getStatus().name());
            stmt.setDouble(5, alert.getUsagePercentage());
            stmt.setTimestamp(6, Timestamp.valueOf(alert.getCreatedAt()));
            stmt.setTimestamp(7, alert.getAcknowledgedAt() != null ? Timestamp.valueOf(alert.getAcknowledgedAt()) : null);
            
            stmt.executeUpdate();
            return alert;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving usage alert", e);
        }
    }
    
    public List<UsageAlert> findActiveAlertsByCustomerId(String customerId) {
        String sql = "SELECT * FROM usage_alerts WHERE customer_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC";
        List<UsageAlert> alerts = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding alerts", e);
        }
        
        return alerts;
    }
    
    public List<UsageAlert> findByCustomerId(String customerId) {
        String sql = "SELECT * FROM usage_alerts WHERE customer_id = ? ORDER BY created_at DESC";
        List<UsageAlert> alerts = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding alerts", e);
        }
        
        return alerts;
    }
    
    private UsageAlert mapResultSetToAlert(ResultSet rs) throws SQLException {
        UsageAlert alert = new UsageAlert();
        alert.setId(rs.getString("id"));
        alert.setCustomerId(rs.getString("customer_id"));
        alert.setAlertType(UsageAlert.AlertType.valueOf(rs.getString("alert_type")));
        alert.setStatus(UsageAlert.AlertStatus.valueOf(rs.getString("status")));
        alert.setUsagePercentage(rs.getDouble("usage_percentage"));
        alert.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp ackTime = rs.getTimestamp("acknowledged_at");
        if (ackTime != null) {
            alert.setAcknowledgedAt(ackTime.toLocalDateTime());
        }
        return alert;
    }
}
