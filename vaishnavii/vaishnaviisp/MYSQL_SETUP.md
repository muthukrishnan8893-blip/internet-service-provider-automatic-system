# MySQL Database Setup Guide

## Prerequisites
- MySQL Server 8.0 or higher installed on your system
- MySQL running on localhost:3306 (default port)

## Setup Steps

### 1. Create Database
Open MySQL command line or MySQL Workbench and run:

```sql
CREATE DATABASE isp_management;
```

### 2. Configure Database Credentials

Edit the file: `src/main/java/com/isp/util/DatabaseConnection.java`

Update these lines with your MySQL credentials:

```java
private static final String DB_USER = "root";      // Your MySQL username
private static final String DB_PASSWORD = "";       // Your MySQL password
```

### 3. Install MySQL (if not installed)

**Windows:**
- Download MySQL Installer from https://dev.mysql.com/downloads/installer/
- Run installer and select "MySQL Server" component
- Set root password during installation
- MySQL will start automatically

**To check if MySQL is running:**
```cmd
mysql -u root -p
```

### 4. Build and Run

```cmd
mvn clean compile
mvn exec:java -Dexec.mainClass=com.isp.Main
```

The application will:
- Connect to MySQL database
- Create all required tables automatically
- Seed default admin and customer accounts

### 5. Default Login Credentials

**Admin:**
- Email: muthuvel04041971@gmail.com
- Password: admin123

**Customer:**
- Email: vaishnavimuthuvel223@gmail.com  
- Password: customer123

## Database Tables Created

The application creates these tables automatically:
- `users` - User accounts (admin/customer)
- `customer_profiles` - Customer profile information
- `data_plans` - Available data plans
- `tickets` - Support tickets
- `ticket_messages` - Ticket conversation messages
- `device_connections` - Customer device connections
- `usage` - Data usage tracking

## Troubleshooting

**Error: Access denied for user**
- Check username and password in DatabaseConnection.java
- Ensure MySQL server is running

**Error: Unknown database 'isp_management'**
- Run `CREATE DATABASE isp_management;` in MySQL

**Error: Communications link failure**
- Verify MySQL is running
- Check port 3306 is not blocked by firewall
- Ensure DB_HOST is set to "localhost"

## Connection String Details

```
Host: localhost
Port: 3306
Database: isp_management
URL: jdbc:mysql://localhost:3306/isp_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

## Viewing Data in MySQL

```sql
USE isp_management;

-- View all users
SELECT * FROM users;

-- View customer profiles
SELECT * FROM customer_profiles;

-- View tickets
SELECT * FROM tickets;
```
