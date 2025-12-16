package com.isp.repo;

import com.isp.model.User;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository for user authentication and management.
 * Now using H2 database for persistence.
 */
public class UserRepository {

    public void save(User user) {
        String sql = """
            INSERT INTO users (id, username, email, password_hash, role, status, last_login) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                email = VALUES(email),
                password_hash = VALUES(password_hash),
                role = VALUES(role),
                status = VALUES(status),
                last_login = VALUES(last_login)
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, user.getStatus().name());
            stmt.setTimestamp(7, user.getLastLogin() != null ? Timestamp.valueOf(user.getLastLogin()) : null);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[UserRepository] Error saving user: " + e.getMessage());
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[UserRepository] Error finding user by id: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[UserRepository] Error finding user by username: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[UserRepository] Error finding user by email: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    public Collection<User> findAll() {
        String sql = "SELECT * FROM users";
        Collection<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[UserRepository] Error finding all users: " + e.getMessage());
        }
        
        return users;
    }

    public void delete(String id) {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[UserRepository] Error deleting user: " + e.getMessage());
        }
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getString("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            User.Role.valueOf(rs.getString("role"))
        );
        
        user.setStatus(User.Status.valueOf(rs.getString("status")));
        
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        return user;
    }
}
