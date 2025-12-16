package com.isp.repo;

import com.isp.model.DataPlan;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository for data plans with database persistence.
 */
public class DataPlanRepository {

    public void save(DataPlan plan) {
        String sql = """
            INSERT INTO data_plans (id, name, data_gb, price_per_month, description, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                data_gb = VALUES(data_gb),
                price_per_month = VALUES(price_per_month),
                description = VALUES(description),
                is_active = VALUES(is_active)
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, plan.getId());
            stmt.setString(2, plan.getName());
            stmt.setDouble(3, plan.getDataGB());
            stmt.setDouble(4, plan.getPricePerMonth());
            stmt.setString(5, plan.getDescription());
            stmt.setBoolean(6, true);
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[DataPlanRepository] Error saving plan: " + e.getMessage());
            throw new RuntimeException("Failed to save data plan", e);
        }
    }

    public Optional<DataPlan> findById(String id) {
        String sql = "SELECT * FROM data_plans WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(new DataPlan(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getDouble("data_gb"),
                    rs.getDouble("price_per_month"),
                    rs.getString("description")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("[DataPlanRepository] Error finding plan by id: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    public Collection<DataPlan> findAll() {
        String sql = "SELECT * FROM data_plans WHERE is_active = true";
        Collection<DataPlan> plans = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                plans.add(new DataPlan(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getDouble("data_gb"),
                    rs.getDouble("price_per_month"),
                    rs.getString("description")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("[DataPlanRepository] Error finding all plans: " + e.getMessage());
        }
        
        return plans;
    }

    public void delete(String id) {
        String sql = "DELETE FROM data_plans WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[DataPlanRepository] Error deleting plan: " + e.getMessage());
        }
    }
}
