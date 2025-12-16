# Email Configuration Guide

## Overview
The ISP Management System now supports sending real emails using Gmail SMTP. By default, emails are simulated (printed to console). To enable real email sending, follow the steps below.

## Email Functionality
- **Admin Email**: muthuvel04041971@gmail.com
- **Customer Email**: vaishnavimuthuvel223@gmail.com

### Email Features:
1. **Registration**: Welcome emails sent to new users
2. **Ticket Creation**: 
   - Customer creates ticket → Admin receives notification
   - Admin email: muthuvel04041971@gmail.com
3. **Ticket Response**:
   - Admin replies → Customer receives email
   - Ticket automatically marked as "In Progress"
4. **Plan Selection**: Confirmation emails
5. **Billing**: Monthly invoice notifications

## Setup Gmail App Password

### Step 1: Enable 2-Factor Authentication
1. Go to your Google Account: https://myaccount.google.com/
2. Click on "Security" in the left sidebar
3. Enable "2-Step Verification" if not already enabled

### Step 2: Generate App Password
1. Go to: https://myaccount.google.com/apppasswords
2. Select app: "Mail"
3. Select device: "Windows Computer"
4. Click "Generate"
5. Copy the 16-character password (format: xxxx xxxx xxxx xxxx)

### Step 3: Set Environment Variable

#### Windows (PowerShell):
```powershell
# Temporary (current session only)
$env:GMAIL_APP_PASSWORD = "your-16-char-password-here"

# Permanent (for your user account)
[System.Environment]::SetEnvironmentVariable('GMAIL_APP_PASSWORD', 'your-16-char-password-here', 'User')
```

#### Windows (Command Prompt):
```cmd
setx GMAIL_APP_PASSWORD "your-16-char-password-here"
```

### Step 4: Restart the Application
```powershell
cd "c:\Users\Asus\Desktop\balaisp\vaishnavii\vaishnaviisp"
mvn clean package
java -jar target/isp-management-0.1.0.jar
```

## Testing Email Functionality

### 1. Test Login
- Email: vaishnavimuthuvel223@gmail.com
- Password: customer123

### 2. Test Ticket System
1. Login as customer (vaishnavimuthuvel223@gmail.com)
2. Navigate to "Tickets" section
3. Create a new ticket with subject and description
4. Click "Submit Ticket"
5. **Result**: Admin (muthuvel04041971@gmail.com) receives email notification

### 3. Test Admin Response
1. Login as admin (muthuvel04041971@gmail.com)
2. Go to "Tickets" in admin panel
3. Select a ticket and add a reply
4. **Result**: Customer receives email, ticket marked as "In Progress"

## Troubleshooting

### Emails Not Sending
- Check if GMAIL_APP_PASSWORD is set: `echo $env:GMAIL_APP_PASSWORD`
- Verify you're using an App Password, not your regular Gmail password
- Check console for "[EMAIL] ✓ Sent to:" messages
- Check spam folder in recipient email

### Authentication Errors
- Ensure 2-Factor Authentication is enabled on the Google account
- Generate a new App Password if the old one doesn't work
- Make sure you're using the Gmail account: muthuvel04041971@gmail.com

## Security Notes
⚠️ **Never commit the App Password to version control**
⚠️ **Keep the App Password secure** - it provides access to your Gmail account
⚠️ **Revoke old App Passwords** you're no longer using from Google Account settings

## Email Simulation Mode
If GMAIL_APP_PASSWORD is not set, the system runs in simulation mode:
- Emails are printed to console with a box border
- Shows "EMAIL NOTIFICATION (SIMULATED)"
- All functionality works except actual email delivery
- Useful for development and testing without email setup
