# ISP Management System - Java Project

A comprehensive ISP management platform that handles customer accounts, tracks network usage, supports troubleshooting, and automates billing.

## Features

- **Customer Management**: Create and manage customer accounts
- **Network Usage Tracking**: Record and monitor customer data usage
- **Ticket System**: Support troubleshooting with a ticket management system
- **Automated Billing**: Calculate bills based on usage with scheduled billing cycles

## Build & Run

### Build the project:
```powershell
mvn clean package
```

### Run the CLI application:
```powershell
java -jar target/isp-management-0.1.0.jar
```

### Run tests:
```powershell
mvn test
```

## Project Structure

- `src/main/java/com/isp/model/` - Domain models (Customer, NetworkUsage, Ticket)
- `src/main/java/com/isp/repo/` - In-memory repositories
- `src/main/java/com/isp/service/` - Business logic services
- `src/main/java/com/isp/Main.java` - CLI entry point
- `src/test/java/` - Unit tests

## Future Enhancements

- Database persistence (JPA/Hibernate)
- REST API with Spring Boot
- Web UI
- Authentication & authorization
- Email notifications
- Advanced billing rules
- Analytics dashboard
