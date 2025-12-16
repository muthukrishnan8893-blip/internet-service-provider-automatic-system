package com.isp.repo;

import com.isp.model.Notification;
import com.isp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing notifications
 */
public class NotificationRepository {
    private DatabaseConnection dbConnection;

    public NotificationRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS notifications (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                type VARCHAR(20) NOT NULL,
                category VARCHAR(50) NOT NULL,
                title VARCHAR(255) NOT NULL,
                message TEXT NOT NULL,
                priority VARCHAR(20) NOT NULL,
                is_read BOOLEAN DEFAULT FALSE,
                is_sent BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                sent_at TIMESTAMP NULL,
                read_at TIMESTAMP NULL,
                metadata TEXT,
                INDEX idx_user_id (user_id),
                INDEX idx_created_at (created_at),
                INDEX idx_is_read (is_read)
            )
        """;

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating notifications table", e);
        }
    }

    public void save(Notification notification) {
        String sql = """
            INSERT INTO notifications 
            (id, user_id, type, category, title, message, priority, is_read, is_sent, created_at, sent_at, read_at, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            message = VALUES(message),
            is_read = VALUES(is_read),
            is_sent = VALUES(is_sent),
            sent_at = VALUES(sent_at),
            read_at = VALUES(read_at)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, notification.getId());
            pstmt.setString(2, notification.getUserId());
            pstmt.setString(3, notification.getType());
            pstmt.setString(4, notification.getCategory());
            pstmt.setString(5, notification.getTitle());
            pstmt.setString(6, notification.getMessage());
            pstmt.setString(7, notification.getPriority());
            pstmt.setBoolean(8, notification.isRead());
            pstmt.setBoolean(9, notification.isSent());
            pstmt.setTimestamp(10, Timestamp.valueOf(notification.getCreatedAt()));
            pstmt.setTimestamp(11, notification.getSentAt() != null ? Timestamp.valueOf(notification.getSentAt()) : null);
            pstmt.setTimestamp(12, notification.getReadAt() != null ? Timestamp.valueOf(notification.getReadAt()) : null);
            pstmt.setString(13, notification.getMetadata());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving notification", e);
        }
    }

    public List<Notification> findByUserId(String userId, int limit) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding notifications", e);
        }

        return notifications;
    }

    public List<Notification> findUnreadByUserId(String userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding unread notifications", e);
        }

        return notifications;
    }

    public void markAsRead(String notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE, read_at = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(2, notificationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking notification as read", e);
        }
    }

    public void markAllAsRead(String userId) {
        String sql = "UPDATE notifications SET is_read = TRUE, read_at = ? WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking all notifications as read", e);
        }
    }

    public int getUnreadCount(String userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting unread count", e);
        }

        return 0;
    }

    public void deleteOldNotifications(int daysOld) {
        String sql = "DELETE FROM notifications WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY) AND is_read = TRUE";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, daysOld);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting old notifications", e);
        }
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getString("id"));
        notification.setUserId(rs.getString("user_id"));
        notification.setType(rs.getString("type"));
        notification.setCategory(rs.getString("category"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setPriority(rs.getString("priority"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setSent(rs.getBoolean("is_sent"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) notification.setCreatedAt(createdAt.toLocalDateTime());
        
        Timestamp sentAt = rs.getTimestamp("sent_at");
        if (sentAt != null) notification.setSentAt(sentAt.toLocalDateTime());
        
        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) notification.setReadAt(readAt.toLocalDateTime());
        
        notification.setMetadata(rs.getString("metadata"));
        
        return notification;
    }
}
