package com.isp.repo;

import com.isp.model.SpeedTest;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for SpeedTest persistence in MySQL
 */
public class SpeedTestRepository {
    
    public SpeedTest save(SpeedTest speedTest) {
        String sql = "INSERT INTO speed_tests (id, customer_id, test_time, download_speed_mbps, upload_speed_mbps, ping_ms) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, speedTest.getId());
            stmt.setString(2, speedTest.getCustomerId());
            stmt.setTimestamp(3, Timestamp.valueOf(speedTest.getTestTime()));
            stmt.setDouble(4, speedTest.getDownloadSpeedMbps());
            stmt.setDouble(5, speedTest.getUploadSpeedMbps());
            stmt.setInt(6, speedTest.getPingMs());
            
            stmt.executeUpdate();
            return speedTest;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving speed test", e);
        }
    }
    
    public List<SpeedTest> findByCustomerId(String customerId, int limit) {
        String sql = "SELECT * FROM speed_tests WHERE customer_id = ? ORDER BY test_time DESC LIMIT ?";
        List<SpeedTest> tests = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customerId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                tests.add(mapResultSetToSpeedTest(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding speed tests", e);
        }
        
        return tests;
    }
    
    public List<SpeedTest> findByCustomerId(String customerId) {
        return findByCustomerId(customerId, 10); // Default limit of 10
    }
    
    private SpeedTest mapResultSetToSpeedTest(ResultSet rs) throws SQLException {
        SpeedTest test = new SpeedTest();
        test.setId(rs.getString("id"));
        test.setCustomerId(rs.getString("customer_id"));
        test.setTestTime(rs.getTimestamp("test_time").toLocalDateTime());
        test.setDownloadSpeedMbps(rs.getDouble("download_speed_mbps"));
        test.setUploadSpeedMbps(rs.getDouble("upload_speed_mbps"));
        test.setPingMs(rs.getInt("ping_ms"));
        return test;
    }
}
