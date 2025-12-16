# Internet Service Provider Automatic System

A comprehensive ISP management platform built with Java that handles customer accounts, tracks network usage, manages devices, automates billing, and provides notification services.

## 🚀 Features

### Core Functionality
- **Customer Management**: Create and manage customer accounts with detailed profiles
- **Network Usage Tracking**: Real-time monitoring of customer data usage across multiple devices
- **Device Management**: Track connected devices with usage logs and connection status
- **Ticket System**: Enhanced support system with priority levels, messaging, and status tracking
- **Automated Billing**: Smart billing system with invoice generation and payment tracking
- **Data Plans**: Flexible data plan management with speed tiers
- **Speed Test**: Network speed testing functionality

### Advanced Features
- **Notification System**: Multi-channel notifications (Email, SMS, Push)
- **Usage Alerts**: Automated alerts for data usage thresholds
- **PDF Invoice Generation**: Professional invoice generation with iText
- **Email Service**: Gmail integration for automated email notifications
- **SMS Service**: SMS notifications for critical alerts
- **Customer Profiles**: Detailed customer preferences and notification settings
- **Daily Usage Tracking**: Granular daily usage statistics
- **Scheduler**: Automated background tasks for billing and notifications

## 🛠️ Technology Stack

- **Backend**: Java 21
- **Build Tool**: Maven
- **Database**: MySQL (with H2 for development)
- **Web Server**: Java HttpServer
- **PDF Generation**: iText
- **Email**: JavaMail API with Gmail
- **Frontend**: HTML, CSS, JavaScript

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6+
- MySQL 8.0+ (or H2 for development)
- Gmail App Password (for email notifications - optional)
- SMS API Key (for SMS notifications - optional)

## 🔧 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/muthukrishnan8893-blip/internet-service-provider-automatic-system.git
   cd internet-service-provider-automatic-system
   ```

2. **Navigate to project directory**
   ```bash
   cd vaishnavii/vaishnaviisp
   ```

3. **Configure MySQL Database** (see [MYSQL_SETUP.md](vaishnavii/vaishnaviisp/MYSQL_SETUP.md))
   ```sql
   CREATE DATABASE isp_management;
   ```

4. **Configure Email (Optional)** (see [EMAIL_SETUP.md](vaishnavii/vaishnaviisp/EMAIL_SETUP.md))
   - Set `GMAIL_APP_PASSWORD` environment variable
   - Configure Gmail account settings

5. **Build the project**
   ```bash
   mvn clean compile
   ```

## 🚀 Running the Application

### Option 1: Using Maven
```bash
mvn clean compile exec:java "-Dexec.mainClass=com.isp.Main"
```

### Option 2: Using VS Code Task
If you're using VS Code, the task "Start ISP Server" is configured to run automatically.

### Access the Application
Once started, access the web interface at:
```
http://localhost:8081
```

## 📁 Project Structure

```
vaishnavii/vaishnaviisp/
├── src/
│   ├── main/
│   │   ├── java/com/isp/
│   │   │   ├── Main.java                    # Application entry point
│   │   │   ├── model/                       # Domain models
│   │   │   │   ├── Customer.java
│   │   │   │   ├── User.java
│   │   │   │   ├── DataPlan.java
│   │   │   │   ├── DeviceConnection.java
│   │   │   │   ├── Invoice.java
│   │   │   │   ├── Notification.java
│   │   │   │   └── ...
│   │   │   ├── repo/                        # Data repositories
│   │   │   │   ├── CustomerRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── ...
│   │   │   ├── service/                     # Business logic
│   │   │   │   ├── CustomerService.java
│   │   │   │   ├── BillingService.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   ├── SmsService.java
│   │   │   │   └── ...
│   │   │   ├── util/                        # Utilities
│   │   │   │   └── DatabaseConnection.java
│   │   │   └── web/                         # Web server & handlers
│   │   │       ├── WebServer.java
│   │   │       └── NotificationHandler.java
│   │   └── resources/
│   │       └── public/                      # Frontend assets
│   │           ├── index.html
│   │           ├── app.js
│   │           └── styles.css
│   └── test/                                # Unit tests
├── pom.xml                                  # Maven configuration
├── README.md
├── MYSQL_SETUP.md                           # Database setup guide
├── EMAIL_SETUP.md                           # Email configuration guide
└── NOTIFICATION_SYSTEM.md                   # Notification system documentation
```

## 🎯 Default Users

The system comes with default users for testing:

**Admin User:**
- Username: `admin`
- Password: `admin123`
- Role: Administrator

**Customer User:**
- Username: `customer1`
- Password: `customer123`
- Role: Customer

## 📚 API Endpoints

### Customer Management
- `GET /api/customers` - Get all customers
- `POST /api/customers` - Create new customer
- `GET /api/customers/{id}` - Get customer by ID
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

### Usage Tracking
- `GET /api/usage` - Get all usage records
- `POST /api/usage` - Record usage
- `GET /api/usage/customer/{customerId}` - Get customer usage

### Billing
- `GET /api/billing/invoices` - Get all invoices
- `POST /api/billing/generate` - Generate invoices
- `GET /api/billing/invoice/{id}/pdf` - Download invoice PDF

### Tickets
- `GET /api/tickets` - Get all tickets
- `POST /api/tickets` - Create ticket
- `PUT /api/tickets/{id}` - Update ticket status

### Devices
- `GET /api/devices` - Get all devices
- `POST /api/devices` - Add device
- `GET /api/devices/customer/{customerId}` - Get customer devices

### Notifications
- `GET /api/notifications` - Get all notifications
- `POST /api/notifications` - Send notification
- `GET /api/notifications/user/{userId}` - Get user notifications

## 🔔 Notification System

The system supports multiple notification channels:

- **Email**: Via Gmail SMTP
- **SMS**: Via SMS API integration
- **Push**: In-app notifications

Notification types:
- Usage alerts (50%, 80%, 100% thresholds)
- Bill generation notifications
- Payment reminders
- Ticket updates
- Service announcements

See [NOTIFICATION_SYSTEM.md](vaishnavii/vaishnaviisp/NOTIFICATION_SYSTEM.md) for details.

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

## 📝 Configuration

### Database Configuration
Edit database settings in `DatabaseConnection.java`:
- Default: H2 in-memory database
- Production: MySQL configuration required

### Email Configuration
Set environment variables:
```bash
export GMAIL_APP_PASSWORD=your_app_password
```

### SMS Configuration
Set environment variables:
```bash
export SMS_API_KEY=your_api_key
```

## 🔐 Security Features

- Password hashing (to be implemented)
- Role-based access control (Admin/Customer)
- Session management
- Input validation
- SQL injection prevention

## 📈 Future Enhancements

- [ ] Advanced analytics dashboard
- [ ] Real-time usage monitoring
- [ ] Multi-language support
- [ ] Mobile application
- [ ] Payment gateway integration
- [ ] Automated service provisioning
- [ ] Customer portal with self-service
- [ ] Advanced reporting and exports
- [ ] Network topology visualization
- [ ] SLA monitoring and enforcement

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is open source and available under the MIT License.

## 👤 Author

**Muthukrishnan**
- GitHub: [@muthukrishnan8893-blip](https://github.com/muthukrishnan8893-blip)

## 📞 Support

For support and queries, please create an issue in the GitHub repository.

## 🙏 Acknowledgments

- Java community for excellent documentation
- iText for PDF generation
- JavaMail for email integration
- All contributors and users of this system

---

**Note**: This is an educational project demonstrating ISP management system capabilities. For production use, additional security hardening and testing are recommended.
