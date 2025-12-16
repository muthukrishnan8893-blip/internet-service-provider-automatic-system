package com.isp.repo;

import com.isp.model.DailyUsage;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DailyUsage persistence in MySQL
 */
public class DailyUsageRepository {
    
    public DailyUsage save(DailyUsage usage) {
        String sql = "INSERT INTO daily_usage (id, customer_id, date, data_used_gb, upload_gb, download_gb, peak_speed_mbps, total_devices_connected) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usage.getId());
            stmt.setString(2, usage.getCustomerId());
            stmt.setTimestamp(3, Timestamp.valueOf(usage.getDate()));
            stmt.setDouble(4, usage.getDataUsedGB());
            stmt.setDouble(5, usage.getUploadGB());
            stmt.setDouble(6, usage.getDownloadGB());
            stmt.setDouble(7, usage.getPeakSpeedMbps());
            stmt.setInt(8, usage.getTotalDevicesConnected());
            
            stmt.executeUpdate();
            return usage;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving daily usage", e);
        }
    }
    
    public List<DailyUsage> findByCustomerIdAndDateRange(String customerId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM daily_usage WHERE customer_id = ? AND date BETWEEN ? AND ? ORDER BY date ASC";
        List<DailyUsage> usageList = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customerId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                usageList.add(mapResultSetToUsage(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding daily usage", e);
        }
        
        return usageList;
    }
    
    public Optional<DailyUsage> findByCustomerIdAndDate(String customerId, LocalDateTime date) {
        String sql = "SELECT * FROM daily_usage WHERE customer_id = ? AND DATE(date) = DATE(?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customerId);
            stmt.setTimestamp(2, Timestamp.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUsage(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding daily usage", e);
        }
        
        return Optional.empty();
    }
    
    private DailyUsage mapResultSetToUsage(ResultSet rs) throws SQLException {
        DailyUsage usage = new DailyUsage();
        usage.setId(rs.getString("id"));
        usage.setCustomerId(rs.getString("customer_id"));
        usage.setDate(rs.getTimestamp("date").toLocalDateTime());
        usage.setDataUsedGB(rs.getDouble("data_used_gb"));
        usage.setUploadGB(rs.getDouble("upload_gb"));
        usage.setDownloadGB(rs.getDouble("download_gb"));
        usage.setPeakSpeedMbps(rs.getDouble("peak_speed_mbps"));
        usage.setTotalDevicesConnected(rs.getInt("total_devices_connected"));
        return usage;
    }
}
