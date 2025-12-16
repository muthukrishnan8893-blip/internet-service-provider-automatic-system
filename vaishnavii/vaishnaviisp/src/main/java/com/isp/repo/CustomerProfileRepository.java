package com.isp.repo;

import com.isp.model.CustomerProfile;
import com.isp.model.DataPlan;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository for customer profiles with database persistence.
 */
public class CustomerProfileRepository {

    public void save(CustomerProfile profile) {
        String sql = """
            INSERT INTO customer_profiles (id, user_id, full_name, is_active, current_plan_id, plan_start_date, plan_renewal_date, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                user_id = VALUES(user_id),
                full_name = VALUES(full_name),
                is_active = VALUES(is_active),
                current_plan_id = VALUES(current_plan_id),
                plan_start_date = VALUES(plan_start_date),
                plan_renewal_date = VALUES(plan_renewal_date)
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, profile.getId());
            stmt.setString(2, profile.getUserId());
            stmt.setString(3, profile.getFullName());
            stmt.setBoolean(4, profile.isActive());
            stmt.setString(5, profile.getCurrentPlan() != null ? profile.getCurrentPlan().getId() : null);
            stmt.setTimestamp(6, profile.getPlanStartDate() != null ? Timestamp.valueOf(profile.getPlanStartDate()) : null);
            stmt.setTimestamp(7, profile.getPlanRenewalDate() != null ? Timestamp.valueOf(profile.getPlanRenewalDate()) : null);
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[CustomerProfileRepository] Error saving profile: " + e.getMessage());
            throw new RuntimeException("Failed to save customer profile", e);
        }
    }

    public Optional<CustomerProfile> findById(String id) {
        String sql = "SELECT * FROM customer_profiles WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToProfile(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[CustomerProfileRepository] Error finding profile by id: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    public Optional<CustomerProfile> findByUserId(String userId) {
        String sql = "SELECT * FROM customer_profiles WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Extract all data first
                String id = rs.getString("id");
                String fullName = rs.getString("full_name");
                boolean isActive = rs.getBoolean("is_active");
                String planId = rs.getString("current_plan_id");
                Timestamp planStart = rs.getTimestamp("plan_start_date");
                Timestamp planRenewal = rs.getTimestamp("plan_renewal_date");
                
                // Close ResultSet before loading plan
                rs.close();
                
                // Build profile object
                CustomerProfile profile = new CustomerProfile(id, userId, fullName);
                profile.setActive(isActive);
                
                // Load plan if exists
                if (planId != null) {
                    try {
                        DataPlan plan = loadDataPlan(planId);
                        if (plan != null) {
                            profile.setCurrentPlan(plan);
                        }
                    } catch (Exception e) {
                        System.err.println("[CustomerProfileRepository] Error loading plan: " + e.getMessage());
                    }
                }
                
                if (planStart != null) {
                    profile.setPlanStartDate(planStart.toLocalDateTime());
                }
                
                if (planRenewal != null) {
                    profile.setPlanRenewalDate(planRenewal.toLocalDateTime());
                }
                
                return Optional.of(profile);
            }
            
        } catch (SQLException e) {
            System.err.println("[CustomerProfileRepository] Error finding profile by user id: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    public Collection<CustomerProfile> findAll() {
        String sql = "SELECT * FROM customer_profiles";
        Collection<CustomerProfile> profiles = new ArrayList<>();
        
        // Store profile data temporarily
        class ProfileData {
            String id, userId, fullName, planId;
            boolean isActive;
            Timestamp planStart, planRenewal;
        }
        java.util.List<ProfileData> dataList = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            // First, extract all data
            while (rs.next()) {
                ProfileData data = new ProfileData();
                data.id = rs.getString("id");
                data.userId = rs.getString("user_id");
                data.fullName = rs.getString("full_name");
                data.isActive = rs.getBoolean("is_active");
                data.planId = rs.getString("current_plan_id");
                data.planStart = rs.getTimestamp("plan_start_date");
                data.planRenewal = rs.getTimestamp("plan_renewal_date");
                dataList.add(data);
            }
            
        } catch (SQLException e) {
            System.err.println("[CustomerProfileRepository] Error finding all profiles: " + e.getMessage());
        }
        
        // Now build profiles with plans loaded
        for (ProfileData data : dataList) {
            CustomerProfile profile = new CustomerProfile(data.id, data.userId, data.fullName);
            profile.setActive(data.isActive);
            
            // Load plan if exists
            if (data.planId != null) {
                try {
                    DataPlan plan = loadDataPlan(data.planId);
                    if (plan != null) {
                        profile.setCurrentPlan(plan);
                    }
                } catch (Exception e) {
                    System.err.println("[CustomerProfileRepository] Error loading plan: " + e.getMessage());
                }
            }
            
            if (data.planStart != null) {
                profile.setPlanStartDate(data.planStart.toLocalDateTime());
            }
            
            if (data.planRenewal != null) {
                profile.setPlanRenewalDate(data.planRenewal.toLocalDateTime());
            }
            
            profiles.add(profile);
        }
        
        return profiles;
    }

    public void delete(String id) {
        String sql = "DELETE FROM customer_profiles WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[CustomerProfileRepository] Error deleting profile: " + e.getMessage());
        }
    }
    
    private CustomerProfile mapResultSetToProfile(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String userId = rs.getString("user_id");
        String fullName = rs.getString("full_name");
        boolean isActive = rs.getBoolean("is_active");
        String planId = rs.getString("current_plan_id");
        Timestamp planStart = rs.getTimestamp("plan_start_date");
        Timestamp planRenewal = rs.getTimestamp("plan_renewal_date");
        
        // Create profile
        CustomerProfile profile = new CustomerProfile(id, userId, fullName);
        profile.setActive(isActive);
        
        // Load current plan if exists (must happen after closing the current ResultSet)
        if (planId != null) {
            try {
                DataPlan plan = loadDataPlan(planId);
                if (plan != null) {
                    profile.setCurrentPlan(plan);
                }
            } catch (Exception e) {
                System.err.println("[CustomerProfileRepository] Error loading plan: " + e.getMessage());
            }
        }
        
        if (planStart != null) {
            profile.setPlanStartDate(planStart.toLocalDateTime());
        }
        
        if (planRenewal != null) {
            profile.setPlanRenewalDate(planRenewal.toLocalDateTime());
        }
        
        return profile;
    }
    
    private DataPlan loadDataPlan(String planId) {
        String sql = "SELECT * FROM data_plans WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, planId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new DataPlan(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getDouble("data_gb"),
                    rs.getDouble("price_per_month"),
                    rs.getString("description")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("[CustomerProfileRepository] Error loading data plan: " + e.getMessage());
        }
        
        return null;
    }
}
