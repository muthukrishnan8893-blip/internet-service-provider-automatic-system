package com.isp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection manager using MySQL Database.
 * Configure MySQL connection details below.
 */
public class DatabaseConnection {
    // MySQL Configuration - Update these values for your MySQL server
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "isp_management";
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "mk93456#";
    private static Connection connection;

    /**
     * Get database connection (creates database if not exists).
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL Driver not found", e);
            }
            
            // First, connect without database to create it if needed
            String baseUrl = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            try (Connection tempConn = DriverManager.getConnection(baseUrl, DB_USER, DB_PASSWORD);
                 Statement stmt = tempConn.createStatement()) {
                stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
                System.out.println("[DATABASE] Database '" + DB_NAME + "' created/verified");
            }
            
            // Now connect to the database
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[DATABASE] Connected to MySQL database: " + DB_NAME);
        }
        return connection;
    }

    /**
     * Initialize database schema (create tables if not exist).
     */
    public static void initializeSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("[DATABASE] Initializing schema...");
            
            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP
                )
            """);
            
            // Customer Profiles table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customer_profiles (
                    id VARCHAR(255) PRIMARY KEY,
                    user_id VARCHAR(255) NOT NULL,
                    full_name VARCHAR(255) NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    current_plan_id VARCHAR(255),
                    plan_start_date TIMESTAMP,
                    plan_renewal_date TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);
            
            // Data Plans table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS data_plans (
                    id VARCHAR(255) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    data_gb DOUBLE NOT NULL,
                    price_per_month DOUBLE NOT NULL,
                    description TEXT,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Tickets table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tickets (
                    id VARCHAR(255) PRIMARY KEY,
                    customer_id VARCHAR(255) NOT NULL,
                    customer_name VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    description TEXT NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    priority VARCHAR(50) DEFAULT 'MEDIUM',
                    assigned_to_admin_id VARCHAR(255),
                    assigned_to_admin_name VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved_at TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES users(id)
                )
            """);
            
            // Ticket Messages table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ticket_messages (
                    id VARCHAR(255) PRIMARY KEY,
                    ticket_id VARCHAR(255) NOT NULL,
                    sender_id VARCHAR(255) NOT NULL,
                    sender_name VARCHAR(255) NOT NULL,
                    message TEXT NOT NULL,
                    message_type VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (ticket_id) REFERENCES tickets(id)
                )
            """);
            
            // Device Connections table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS device_connections (
                    id VARCHAR(255) PRIMARY KEY,
                    customer_id VARCHAR(255) NOT NULL,
                    device_name VARCHAR(255) NOT NULL,
                    mac_address VARCHAR(50) NOT NULL,
                    connect_time TIMESTAMP NOT NULL,
                    disconnect_time TIMESTAMP,
                    data_used_gb DOUBLE DEFAULT 0,
                    is_active BOOLEAN DEFAULT TRUE,
                    FOREIGN KEY (customer_id) REFERENCES customer_profiles(id)
                )
            """);
            
            // Usage table (backticks around table name because 'usage' is a MySQL reserved word)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `usage` (
                    id VARCHAR(255) PRIMARY KEY,
                    customer_id VARCHAR(255) NOT NULL,
                    gigabytes DOUBLE NOT NULL,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES customer_profiles(id)
                )
            """);
            
            // Daily Usage table for tracking daily statistics
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS daily_usage (
                    id VARCHAR(255) PRIMARY KEY,
                    customer_id VARCHAR(255) NOT NULL,
                    date DATE NOT NULL,
                    data_used_gb DOUBLE DEFAULT 0,
                    upload_gb DOUBLE DEFAULT 0,
                    download_gb DOUBLE DEFAULT 0,
                    peak_speed_mbps DOUBLE DEFAULT 0,
                    total_devices_connected INT DEFAULT 0,
                    FOREIGN KEY (customer_id) REFERENCES customer_profiles(id),
                    UNIQUE KEY unique_customer_date (customer_id, date)
                )
            """);
            
            System.out.println("[DATABASE] Schema initialized successfully!");
            
        } catch (SQLException e) {
            System.err.println("[DATABASE] Error initializing schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Close database connection.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DATABASE] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE] Error closing connection: " + e.getMessage());
        }
    }
}
