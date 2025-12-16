package com.isp.repo;

import com.isp.model.NotificationPreferences;
import com.isp.util.DatabaseConnection;

import java.sql.*;

/**
 * Repository for managing notification preferences
 */
public class NotificationPreferencesRepository {
    private DatabaseConnection dbConnection;

    public NotificationPreferencesRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS notification_preferences (
                user_id VARCHAR(36) PRIMARY KEY,
                email_enabled BOOLEAN DEFAULT TRUE,
                email_usage_alerts BOOLEAN DEFAULT TRUE,
                email_payment_reminders BOOLEAN DEFAULT TRUE,
                email_ticket_updates BOOLEAN DEFAULT TRUE,
                email_security_alerts BOOLEAN DEFAULT TRUE,
                email_promotions BOOLEAN DEFAULT FALSE,
                browser_enabled BOOLEAN DEFAULT TRUE,
                browser_usage_alerts BOOLEAN DEFAULT TRUE,
                browser_payment_reminders BOOLEAN DEFAULT TRUE,
                browser_ticket_updates BOOLEAN DEFAULT TRUE,
                browser_security_alerts BOOLEAN DEFAULT TRUE,
                sms_enabled BOOLEAN DEFAULT FALSE,
                sms_critical_only BOOLEAN DEFAULT TRUE,
                sms_usage_alerts BOOLEAN DEFAULT FALSE,
                sms_payment_reminders BOOLEAN DEFAULT FALSE,
                sms_security_alerts BOOLEAN DEFAULT TRUE,
                phone_number VARCHAR(20),
                usage_alert_threshold_1 INT DEFAULT 50,
                usage_alert_threshold_2 INT DEFAULT 75,
                usage_alert_threshold_3 INT DEFAULT 90
            )
        """;

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating notification_preferences table", e);
        }
    }

    public void save(NotificationPreferences prefs) {
        String sql = """
            INSERT INTO notification_preferences 
            (user_id, email_enabled, email_usage_alerts, email_payment_reminders, email_ticket_updates, 
             email_security_alerts, email_promotions, browser_enabled, browser_usage_alerts, 
             browser_payment_reminders, browser_ticket_updates, browser_security_alerts, 
             sms_enabled, sms_critical_only, sms_usage_alerts, sms_payment_reminders, 
             sms_security_alerts, phone_number, usage_alert_threshold_1, usage_alert_threshold_2, 
             usage_alert_threshold_3)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            email_enabled = VALUES(email_enabled),
            email_usage_alerts = VALUES(email_usage_alerts),
            email_payment_reminders = VALUES(email_payment_reminders),
            email_ticket_updates = VALUES(email_ticket_updates),
            email_security_alerts = VALUES(email_security_alerts),
            email_promotions = VALUES(email_promotions),
            browser_enabled = VALUES(browser_enabled),
            browser_usage_alerts = VALUES(browser_usage_alerts),
            browser_payment_reminders = VALUES(browser_payment_reminders),
            browser_ticket_updates = VALUES(browser_ticket_updates),
            browser_security_alerts = VALUES(browser_security_alerts),
            sms_enabled = VALUES(sms_enabled),
            sms_critical_only = VALUES(sms_critical_only),
            sms_usage_alerts = VALUES(sms_usage_alerts),
            sms_payment_reminders = VALUES(sms_payment_reminders),
            sms_security_alerts = VALUES(sms_security_alerts),
            phone_number = VALUES(phone_number),
            usage_alert_threshold_1 = VALUES(usage_alert_threshold_1),
            usage_alert_threshold_2 = VALUES(usage_alert_threshold_2),
            usage_alert_threshold_3 = VALUES(usage_alert_threshold_3)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, prefs.getUserId());
            pstmt.setBoolean(2, prefs.isEmailEnabled());
            pstmt.setBoolean(3, prefs.isEmailUsageAlerts());
            pstmt.setBoolean(4, prefs.isEmailPaymentReminders());
            pstmt.setBoolean(5, prefs.isEmailTicketUpdates());
            pstmt.setBoolean(6, prefs.isEmailSecurityAlerts());
            pstmt.setBoolean(7, prefs.isEmailPromotions());
            pstmt.setBoolean(8, prefs.isBrowserEnabled());
            pstmt.setBoolean(9, prefs.isBrowserUsageAlerts());
            pstmt.setBoolean(10, prefs.isBrowserPaymentReminders());
            pstmt.setBoolean(11, prefs.isBrowserTicketUpdates());
            pstmt.setBoolean(12, prefs.isBrowserSecurityAlerts());
            pstmt.setBoolean(13, prefs.isSmsEnabled());
            pstmt.setBoolean(14, prefs.isSmsCriticalOnly());
            pstmt.setBoolean(15, prefs.isSmsUsageAlerts());
            pstmt.setBoolean(16, prefs.isSmsPaymentReminders());
            pstmt.setBoolean(17, prefs.isSmsSecurityAlerts());
            pstmt.setString(18, prefs.getPhoneNumber());
            pstmt.setInt(19, prefs.getUsageAlertThreshold1());
            pstmt.setInt(20, prefs.getUsageAlertThreshold2());
            pstmt.setInt(21, prefs.getUsageAlertThreshold3());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving notification preferences", e);
        }
    }

    public NotificationPreferences findByUserId(String userId) {
        String sql = "SELECT * FROM notification_preferences WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPreferences(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding notification preferences", e);
        }

        // Return default preferences if not found
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setUserId(userId);
        return prefs;
    }

    private NotificationPreferences mapResultSetToPreferences(ResultSet rs) throws SQLException {
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setUserId(rs.getString("user_id"));
        prefs.setEmailEnabled(rs.getBoolean("email_enabled"));
        prefs.setEmailUsageAlerts(rs.getBoolean("email_usage_alerts"));
        prefs.setEmailPaymentReminders(rs.getBoolean("email_payment_reminders"));
        prefs.setEmailTicketUpdates(rs.getBoolean("email_ticket_updates"));
        prefs.setEmailSecurityAlerts(rs.getBoolean("email_security_alerts"));
        prefs.setEmailPromotions(rs.getBoolean("email_promotions"));
        prefs.setBrowserEnabled(rs.getBoolean("browser_enabled"));
        prefs.setBrowserUsageAlerts(rs.getBoolean("browser_usage_alerts"));
        prefs.setBrowserPaymentReminders(rs.getBoolean("browser_payment_reminders"));
        prefs.setBrowserTicketUpdates(rs.getBoolean("browser_ticket_updates"));
        prefs.setBrowserSecurityAlerts(rs.getBoolean("browser_security_alerts"));
        prefs.setSmsEnabled(rs.getBoolean("sms_enabled"));
        prefs.setSmsCriticalOnly(rs.getBoolean("sms_critical_only"));
        prefs.setSmsUsageAlerts(rs.getBoolean("sms_usage_alerts"));
        prefs.setSmsPaymentReminders(rs.getBoolean("sms_payment_reminders"));
        prefs.setSmsSecurityAlerts(rs.getBoolean("sms_security_alerts"));
        prefs.setPhoneNumber(rs.getString("phone_number"));
        prefs.setUsageAlertThreshold1(rs.getInt("usage_alert_threshold_1"));
        prefs.setUsageAlertThreshold2(rs.getInt("usage_alert_threshold_2"));
        prefs.setUsageAlertThreshold3(rs.getInt("usage_alert_threshold_3"));
        return prefs;
    }
}
