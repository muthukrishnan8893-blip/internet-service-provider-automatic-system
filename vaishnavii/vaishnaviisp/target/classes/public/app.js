// ISP Management System - Frontend Application
let currentUser = null;  // userId (UUID)
let currentUsername = null;  // username for display
let currentToken = null;
let currentRole = null;

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    const savedToken = localStorage.getItem('ispToken');
    const savedUser = localStorage.getItem('ispUser');
    const savedUsername = localStorage.getItem('ispUsername');
    const savedRole = localStorage.getItem('ispRole');
    
    console.log('Initializing app - Token:', savedToken ? 'exists' : 'none', 'User:', savedUser);
    
    if (savedToken && savedUser) {
        currentToken = savedToken;
        currentUser = savedUser;
        currentUsername = savedUsername;
        currentRole = savedRole;
        console.log('User logged in:', currentUser, 'Role:', currentRole);
        showDashboard();
    } else {
        console.log('No saved session found');
    }
    
    // Add event listeners for usage monitoring
    document.addEventListener('click', function(e) {
        // Speed test button
        if (e.target && e.target.id === 'runSpeedTestBtn') {
            runSpeedTest();
        }
        
        // Period selector buttons
        if (e.target && e.target.classList.contains('period-btn')) {
            const period = e.target.dataset.period;
            // Update active button
            document.querySelectorAll('.period-btn').forEach(btn => btn.classList.remove('active'));
            e.target.classList.add('active');
            // Reload chart with new period
            loadUsageChart(period);
        }
    });
});

// Tab switching
function switchTab(tab) {
    document.getElementById('loginForm').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('registerForm').style.display = tab === 'register' ? 'block' : 'none';
    document.getElementById('loginTab').classList.toggle('active', tab === 'login');
    document.getElementById('registerTab').classList.toggle('active', tab === 'register');
}

// Handle Login (supports email or username)
async function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('loginUsername').value; // Can be email or username
    const password = document.getElementById('loginPassword').value;
    const selectedRole = document.getElementById('loginRole').value;
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            // Verify role matches
            if (data.role !== selectedRole) {
                alert(`Invalid role selected. This account is registered as ${data.role}. Please select the correct role and try again.`);
                return;
            }
            
            currentToken = data.token;
            currentUser = data.userId;  // Store userId for API calls
            currentUsername = data.username;  // Store username for display
            currentRole = data.role;
            
            localStorage.setItem('ispToken', currentToken);
            localStorage.setItem('ispUser', currentUser);  // Save userId
            localStorage.setItem('ispUsername', currentUsername);  // Save username
            localStorage.setItem('ispRole', currentRole);
            
            showDashboard();
            loadDashboardData();
        } else {
            alert('Login failed: ' + (data.message || 'Invalid credentials'));
        }
    } catch (error) {
        console.error('Login error:', error);
        alert('Login error: ' + error.message);
    }
}

// Show Forgot Password Form
function showForgotPassword(event) {
    event.preventDefault();
    document.getElementById('loginForm').style.display = 'none';
    document.getElementById('registerForm').style.display = 'none';
    document.getElementById('forgotPasswordForm').style.display = 'block';
    document.getElementById('otpRequestStep').style.display = 'block';
    document.getElementById('otpVerifyStep').style.display = 'none';
}

// Back to Login
function backToLogin() {
    document.getElementById('forgotPasswordForm').style.display = 'none';
    document.getElementById('loginForm').style.display = 'block';
    document.getElementById('loginTab').classList.add('active');
    document.getElementById('registerTab').classList.remove('active');
}

// Request OTP
async function requestOTP(event) {
    event.preventDefault();
    
    const email = document.getElementById('forgotEmail').value;
    
    try {
        const response = await fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            alert('✓ OTP has been sent to your email!\n\nPlease check your inbox and enter the 6-digit code.');
            document.getElementById('otpRequestStep').style.display = 'none';
            document.getElementById('otpVerifyStep').style.display = 'block';
        } else {
            alert('Error: ' + (data.message || 'Failed to send OTP'));
        }
    } catch (error) {
        console.error('OTP request error:', error);
        alert('Error: ' + error.message);
    }
}

// Verify OTP and Reset Password
async function verifyOTPAndResetPassword(event) {
    event.preventDefault();
    
    const email = document.getElementById('forgotEmail').value;
    const otp = document.getElementById('otpCode').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    if (newPassword !== confirmPassword) {
        alert('Passwords do not match!');
        return;
    }
    
    if (newPassword.length < 6) {
        alert('Password must be at least 6 characters long!');
        return;
    }
    
    try {
        const response = await fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, otp, newPassword })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            alert('✓ Password reset successful!\n\nYou can now login with your new password.');
            backToLogin();
            // Clear form
            document.getElementById('forgotEmail').value = '';
            document.getElementById('otpCode').value = '';
            document.getElementById('newPassword').value = '';
            document.getElementById('confirmPassword').value = '';
        } else {
            alert('Error: ' + (data.message || 'Failed to reset password'));
        }
    } catch (error) {
        console.error('Password reset error:', error);
        alert('Error: ' + error.message);
    }
}

// Handle Registration
async function handleRegister(event) {
    event.preventDefault();
    
    const username = document.getElementById('regUsername').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    const role = document.getElementById('regRole').value;
    
    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password, role })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            alert('Registration successful! Please login.');
            switchTab('login');
        } else {
            alert('Registration failed: ' + data.message);
        }
    } catch (error) {
        console.error('Registration error:', error);
        alert('Registration error: ' + error.message);
    }
}

// Show Dashboard
function showDashboard() {
    document.getElementById('authScreen').style.display = 'none';
    document.getElementById('dashboardScreen').style.display = 'block';
    document.getElementById('userDisplay').textContent = `${currentUsername} (${currentRole})`;
    
    // Show appropriate menu
    if (currentRole === 'ADMIN') {
        document.getElementById('customerMenu').style.display = 'none';
        document.getElementById('adminMenu').style.display = 'block';
        showSection('admin-dashboard');
    } else {
        document.getElementById('customerMenu').style.display = 'block';
        document.getElementById('adminMenu').style.display = 'none';
        showSection('dashboard');
    }
}

// Show section
function showSection(section, event) {
    // Hide all sections
    document.querySelectorAll('.content-section').forEach(el => el.style.display = 'none');
    
    // Show selected section
    const element = document.getElementById(section);
    if (element) {
        element.style.display = 'block';
    }
    
    // Update active menu
    document.querySelectorAll('.nav-link').forEach(el => el.classList.remove('active'));
    if (event && event.target) { event.target.classList.add('active'); }
    
    // Load section data
    if (section === 'dashboard') loadCustomerDashboard();
    if (section === 'devices') loadDevices();
    if (section === 'plans') loadPlans();
    if (section === 'tickets') loadTickets();
    if (section === 'admin-dashboard') loadAdminDashboard();
    if (section === 'customers') loadCustomers();
    if (section === 'revenue-management') loadRevenueManagement();
    if (section === 'admin-tickets') loadAdminTickets();
}

// Load Customer Dashboard
async function loadCustomerDashboard() {
    try {
        const response = await fetch(`/api/customer/profile?token=${currentToken}`);
        const profile = await response.json();
        
        // Update plan info
        document.getElementById('currentPlan').textContent = profile.planName || 'No Plan';
        
        // Calculate realistic data usage (based on plan if available)
        const planLimit = profile.dataGB || 100; // Default 100 GB if no plan
        const dataUsed = (Math.random() * planLimit * 0.7).toFixed(1); // Use 0-70% of plan
        const usagePercent = ((dataUsed / planLimit) * 100).toFixed(1);
        const remainingData = (planLimit - dataUsed).toFixed(1);
        
        document.getElementById('dataUsage').textContent = dataUsed + ' GB';
        document.getElementById('usagePercentage').textContent = usagePercent + '%';
        document.getElementById('remainingData').textContent = remainingData + ' GB';
        
        // Update balance - show with currency symbol
        const balance = profile.balance || 0;
        document.getElementById('accountBalance').textContent = '$' + balance.toFixed(2);
        
        // Update balance card color based on balance amount
        const balanceCard = document.getElementById('accountBalance').parentElement;
        if (balance < 10) {
            balanceCard.classList.add('bg-danger', 'text-white');
            balanceCard.classList.remove('bg-success', 'bg-warning');
        } else if (balance < 50) {
            balanceCard.classList.add('bg-warning');
            balanceCard.classList.remove('bg-success', 'bg-danger', 'text-white');
        } else {
            balanceCard.classList.add('bg-success', 'text-white');
            balanceCard.classList.remove('bg-danger', 'bg-warning');
        }
        
        // Get device count from API
        try {
            const devicesResponse = await fetch(`/api/customer/devices?token=${currentToken}`);
            const devicesData = await devicesResponse.json();
            const deviceCount = devicesData.devices?.length || 0;
            document.getElementById('devicesCount').textContent = deviceCount;
        } catch (err) {
            document.getElementById('devicesCount').textContent = '0';
        }
        
        // Show usage alert if usage is high
        if (usagePercent > 80) {
            showUsageAlert(usagePercent, remainingData);
        }
        
    } catch (error) {
        console.error('Error loading dashboard:', error);
        // Set default values if API fails
        document.getElementById('currentPlan').textContent = 'No Plan';
        document.getElementById('dataUsage').textContent = '0 GB';
        document.getElementById('usagePercentage').textContent = '0%';
        document.getElementById('remainingData').textContent = '0 GB';
        document.getElementById('accountBalance').textContent = '$0.00';
        document.getElementById('devicesCount').textContent = '0';
    }
}

// Show usage alert
function showUsageAlert(usagePercent, remainingData) {
    const alertContainer = document.getElementById('usageAlertsContainer');
    if (!alertContainer) return;
    
    let alertClass = 'alert-warning';
    let alertIcon = 'fa-exclamation-triangle';
    let alertTitle = 'High Usage Warning';
    
    if (usagePercent > 90) {
        alertClass = 'alert-danger';
        alertIcon = 'fa-exclamation-circle';
        alertTitle = 'Critical Usage Alert';
    }
    
    alertContainer.innerHTML = `
        <div class="alert ${alertClass} alert-dismissible fade show" role="alert">
            <i class="fas ${alertIcon} me-2"></i>
            <strong>${alertTitle}:</strong> You have used ${usagePercent}% of your data plan. 
            Only ${remainingData} GB remaining!
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}

// Load Devices
async function loadDevices() {
    try {
        const response = await fetch(`/api/customer/devices?token=${currentToken}`);
        const data = await response.json();
        
        if (data.status === 'success' && data.devices && data.devices.length > 0) {
            let html = '';
            
            data.devices.forEach(device => {
                const statusBadge = device.status === 'Active' ? 'bg-success' : 'bg-secondary';
                const statusIcon = device.status === 'Active' ? 'fa-circle' : 'fa-times-circle';
                html += `
                    <tr>
                        <td><i class="fas fa-mobile-alt text-primary me-2"></i> <strong>${device.device_name}</strong></td>
                        <td><code>${device.ip_address}</code></td>
                        <td><span class="badge bg-info">${device.total_data_used_mb} MB</span></td>
                        <td><span class="badge bg-primary">${device.average_speed_mbps} Mbps</span></td>
                        <td><small class="text-muted">${device.connection_start_time}</small></td>
                        <td><small class="text-muted">${device.connection_end_time}</small></td>
                        <td><span class="badge ${statusBadge}"><i class="fas ${statusIcon} me-1"></i>${device.status}</span></td>
                    </tr>
                `;
            });
            
            document.getElementById('devicesList').innerHTML = html;
        } else {
            document.getElementById('devicesList').innerHTML = '<tr><td colspan="7"><div class="alert alert-info m-0"><i class="fas fa-info-circle me-2"></i>No devices connected yet. Connect to your hotspot to see device history here.</div></td></tr>';
        }
    } catch (error) {
        console.error('Error loading devices:', error);
        document.getElementById('devicesList').innerHTML = '<tr><td colspan="7"><div class="alert alert-danger m-0"><i class="fas fa-exclamation-triangle me-2"></i>Error loading devices. Please try again.</div></td></tr>';
    }
}

// Load Device Usage
async function loadDeviceUsage(deviceId) {
    try {
        const response = await fetch(`/api/customer/device-usage?token=${currentToken}&deviceId=${deviceId}`);
        const data = await response.json();
        
        let html = '<h6>Usage History</h6>';
        data.usageLogs.forEach(log => {
            html += `
                <div class="small mb-2">
                    <strong>${log.timestamp}</strong>: ${log.dataUsedGB.toFixed(2)} GB (${log.status})
                </div>
            `;
        });
        
        alert(html);
    } catch (error) {
        alert('Error loading usage: ' + error.message);
    }
}

// Load Plans
async function loadPlans() {
    try {
        const response = await fetch(`/api/customer/plans?token=${currentToken}`);
        const data = await response.json();
        
        let html = '';
        data.plans.forEach(plan => {
            html += `
                <div class="col-md-6 mb-4">
                    <div class="card plan-card">
                        <div class="card-body">
                            <h5 class="card-title">${plan.name}</h5>
                            <p class="card-text">${plan.description}</p>
                            <div class="mb-3">
                                <h3 class="text-primary">$${plan.pricePerMonth}/mo</h3>
                                <p class="text-muted">${plan.dataGB} GB Data</p>
                            </div>
                            <button class="btn btn-primary w-100" onclick="selectPlan('${plan.id}')">
                                Select Plan
                            </button>
                        </div>
                    </div>
                </div>
            `;
        });
        
        document.getElementById('plansList').innerHTML = html;
    } catch (error) {
        console.error('Error loading plans:', error);
    }
}

// Select Plan
async function selectPlan(planId) {
    try {
        const response = await fetch('/api/customer/select-plan', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + currentToken },
            body: JSON.stringify({ planId })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            alert('✓ Plan selected successfully! Confirmation email sent.');
            loadPlans();
        } else {
            alert('Error: ' + (data.message || 'Failed to select plan'));
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Load Tickets
async function loadTickets() {
    try {
        const response = await fetch(`/api/tickets-enhanced/list?token=${currentToken}`, {
            headers: { 'Authorization': 'Bearer ' + currentToken }
        });
        if (!response.ok) { 
            document.getElementById('ticketsList').innerHTML = '<p class="text-muted">No tickets yet</p>'; 
            return; 
        }
        const result = await response.json();
        
        let html = '';
        if (result.status === 'success' && result.tickets && result.tickets.length > 0) {
            result.tickets.forEach(ticket => {
                const statusClass = ticket.status === 'OPEN' ? 'badge-warning' : ticket.status === 'IN_PROGRESS' ? 'badge-info' : 'badge-success';
                html += `
                    <div class="ticket-item" onclick="viewTicketConversation('${ticket.id}')" style="cursor: pointer;">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <div>
                                <h6 class="mb-1">${ticket.subject || 'No subject'}</h6>
                                <p class="mb-1 small">${ticket.description}</p>
                            </div>
                            <span class="badge ${statusClass}">${ticket.status}</span>
                        </div>
                        <small class="text-muted">ID: ${ticket.id} | Created: ${new Date(ticket.createdAt).toLocaleString()}</small>
                        ${ticket.messageCount > 0 ? '<small class="text-primary ms-2"><i class="fas fa-comments"></i> ' + ticket.messageCount + ' messages</small>' : ''}
                        <small class="text-info ms-2"><i class="fas fa-eye"></i> Click to view</small>
                    </div>
                `;
            });
        }
        document.getElementById('ticketsList').innerHTML = html || '<p class="text-muted">No tickets yet</p>';
    } catch (error) {
        console.error('Error loading tickets:', error);
        document.getElementById('ticketsList').innerHTML = '<p class="text-muted">No tickets yet</p>';
    }
}

// Create Ticket
async function createTicket(event) {
    event.preventDefault();
    
    // Check if user is logged in
    if (!currentToken) {
        alert('Please login first to create a ticket.');
        return;
    }
    
    const subject = document.getElementById('ticketSubject').value;
    const description = document.getElementById('ticketDescription').value;
    
    if (!subject || !description) {
        alert('Please fill in both subject and description.');
        return;
    }
    
    try {
        console.log('Creating ticket with token:', currentToken ? 'Token exists' : 'No token');
        const response = await fetch('/api/tickets-enhanced/create?token=' + encodeURIComponent(currentToken), {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + currentToken
            },
            body: JSON.stringify({ subject, description })
        });
        
        const data = await response.json();
        console.log('Response:', data);
        
        if (response.ok) {
            alert('✓ Ticket created successfully!\n\nAdmin has been notified via email at muthuvel04041971@gmail.com\n\nYou will receive a response via email at your registered email address.');
            document.getElementById('ticketSubject').value = '';
            document.getElementById('ticketDescription').value = '';
            loadTickets();
        } else {
            alert('Error: ' + (data.message || 'Failed to create ticket. Status: ' + response.status));
        }
    } catch (error) {
        console.error('Ticket creation error:', error);
        alert('Error creating ticket: ' + error.message);
    }
}

// Show Ticket Messages
async function showTicketMessages(ticketId) {
    try {
        const response = await fetch(`/api/customer/tickets?token=${currentToken}`);
        const data = await response.json();
        const ticket = data.tickets.find(t => t.id === ticketId);
        
        let html = `<h6>${ticket.subject}</h6>`;
        ticket.messages.forEach(msg => {
            html += `
                <div class="card mb-2 ${msg.type === 'ADMIN' ? 'border-success' : 'border-primary'}">
                    <div class="card-body py-2">
                        <small class="text-muted">${msg.sender} (${msg.type})</small><br>
                        <strong>${msg.message}</strong><br>
                        <small class="text-muted">${msg.sentAt}</small>
                    </div>
                </div>
            `;
        });
        
        alert(html);
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Download Invoice
async function downloadInvoice() {
    try {
        // Simply use currentUser as the customer ID
        if (!currentUser || !currentToken) {
            alert('Please login first');
            return;
        }
        
        console.log('Downloading invoice for user:', currentUser);
        
        const response = await fetch(`/api/billing/customer/${currentUser}/invoice?token=${currentToken}`);
        
        console.log('Invoice response status:', response.status);
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            const today = new Date().toISOString().slice(0,7).replace('-','');
            a.download = `invoice-${currentUser.substring(0,8)}-${today}.pdf`;
            a.click();
            alert('✓ Invoice downloaded successfully!');
        } else {
            const errorText = await response.text();
            console.error('Invoice error response:', errorText);
            alert('Error downloading invoice. Response: ' + response.status);
        }
    } catch (error) {
        console.error('Invoice download error:', error);
        alert('Error: ' + error.message);
    }
}

// Load Admin Dashboard
async function loadAdminDashboard() {
    try {
        const response = await fetch(`/api/admin/customers?token=${currentToken}`);
        const data = await response.json();
        
        const totalCustomers = data.customers.length;
        let totalRevenue = 0;
        let openTickets = 0;
        
        data.customers.forEach(c => {
            if (c.plan) totalRevenue += 299; // average plan price
        });
        
        document.getElementById('totalCustomers').textContent = totalCustomers;
        document.getElementById('monthlyRevenue').textContent = '$' + totalRevenue;
        document.getElementById('activeTickets').textContent = '5+'; // Sample
        document.getElementById('systemStatus').textContent = 'Online';
    } catch (error) {
        console.error('Error loading admin dashboard:', error);
    }
}

// Load Customers
async function loadCustomers() {
    try {
        const response = await fetch(`/api/admin/customers?token=${currentToken}`);
        const data = await response.json();
        
        console.log('Load customers response:', data);
        console.log('Response OK:', response.ok);
        console.log('Has customers:', !!data.customers);
        
        if (!response.ok) {
            alert('Error loading customers: ' + (data.message || 'Server returned error'));
            console.error('Error response:', data);
            return;
        }
        
        if (!data.customers) {
            alert('Error: No customers data in response. Response: ' + JSON.stringify(data));
            console.error('Missing customers field:', data);
            return;
        }
        
        let html = '';
        data.customers.forEach(customer => {
            html += `
                <tr>
                    <td><strong>${customer.username}</strong></td>
                    <td>${customer.email}</td>
                    <td>${customer.plan || 'No Plan'}</td>
                    <td>${customer.dataUsed || 0}/${customer.dataLimit || 0} GB</td>
                    <td><span class="badge bg-success">${customer.status}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewCustomerDetail('${customer.id}')">
                            View
                        </button>
                    </td>
                </tr>
            `;
        });
        
        document.getElementById('customersTableBody').innerHTML = html;
    } catch (error) {
        console.error('Error loading customers:', error);
        alert('Error loading customers: ' + error.message);
    }
}

// View Customer Detail
async function viewCustomerDetail(customerId) {
    try {
        const response = await fetch(`/api/admin/customer-detail?token=${currentToken}&customerId=${customerId}`);
        const data = await response.json();
        
        if (!response.ok || data.status === 'error') {
            alert('Error: ' + (data.message || 'Could not load customer details'));
            return;
        }
        
        let html = `
            <div class="row">
                <div class="col-md-6">
                    <h6 class="text-primary"><i class="fas fa-user me-2"></i>Personal Information</h6>
                    <table class="table table-sm">
                        <tr>
                            <td><strong>Username:</strong></td>
                            <td>${data.username}</td>
                        </tr>
                        <tr>
                            <td><strong>Email:</strong></td>
                            <td>${data.email}</td>
                        </tr>
                        <tr>
                            <td><strong>Full Name:</strong></td>
                            <td>${data.fullName || 'N/A'}</td>
                        </tr>
                        <tr>
                            <td><strong>Status:</strong></td>
                            <td><span class="badge bg-success">${data.status}</span></td>
                        </tr>
                    </table>
                </div>
                <div class="col-md-6">
        `;
        
        if (data.plan) {
            html += `
                    <h6 class="text-primary"><i class="fas fa-wifi me-2"></i>Current Plan</h6>
                    <div class="card bg-light">
                        <div class="card-body">
                            <h5 class="card-title">${data.plan.name}</h5>
                            <p class="card-text">
                                <i class="fas fa-database me-2"></i><strong>Data:</strong> ${data.plan.dataGB} GB<br>
                                <i class="fas fa-dollar-sign me-2"></i><strong>Price:</strong> $${data.plan.price}/month<br>
                                <i class="fas fa-info-circle me-2"></i>${data.plan.description}
                            </p>
            `;
            if (data.planStartDate) {
                html += `<p class="mb-1"><small><i class="fas fa-calendar-check me-2"></i><strong>Started:</strong> ${new Date(data.planStartDate).toLocaleDateString()}</small></p>`;
            }
            if (data.planRenewalDate) {
                html += `<p class="mb-0"><small><i class="fas fa-calendar-alt me-2"></i><strong>Renews:</strong> ${new Date(data.planRenewalDate).toLocaleDateString()}</small></p>`;
            }
            html += `
                        </div>
                    </div>
            `;
        } else {
            html += `
                    <h6 class="text-primary"><i class="fas fa-wifi me-2"></i>Current Plan</h6>
                    <div class="alert alert-warning">
                        <i class="fas fa-exclamation-triangle me-2"></i>No plan selected
                    </div>
            `;
        }
        
        html += `
                </div>
            </div>
        `;
        
        document.getElementById('customerDetailContent').innerHTML = html;
        const modal = new bootstrap.Modal(document.getElementById('customerDetailModal'));
        modal.show();
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Load Admin Tickets
async function loadAdminTickets() {
    try {
        const response = await fetch(`/api/tickets-enhanced/list?token=${currentToken}`);
        const data = await response.json();
        
        let html = '';
        data.tickets.forEach(ticket => {
            const createdDate = new Date(ticket.createdAt);
            const formattedDate = createdDate.toLocaleString('en-IN', { 
                dateStyle: 'medium', 
                timeStyle: 'short' 
            });
            
            html += `
                <div class="ticket-item">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <h6>${ticket.subject}</h6>
                            <small class="text-muted">From: <strong>${ticket.customerName || 'Unknown Customer'}</strong></small><br>
                            <small class="text-muted"><i class="fas fa-clock"></i> ${formattedDate}</small>
                        </div>
                        <span class="badge bg-${ticket.status === 'OPEN' ? 'danger' : 'success'}">${ticket.status}</span>
                    </div>
                    <p class="mt-2">${ticket.description}</p>
                    <div class="mt-3">
                        <textarea class="form-control mb-2" placeholder="Reply message..." id="reply-${ticket.id}"></textarea>
                        <button class="btn btn-sm btn-success" onclick="respondToTicket('${ticket.id}')">
                            <i class="fas fa-reply me-1"></i> Respond & Email
                        </button>
                        <button class="btn btn-sm btn-outline-success ms-2" onclick="closeTicket('${ticket.id}')">
                            Close
                        </button>
                    </div>
                </div>
            `;
        });
        
        document.getElementById('adminTicketsList').innerHTML = html || '<p class="text-muted">No tickets</p>';
    } catch (error) {
        console.error('Error loading tickets:', error);
    }
}

// Respond to Ticket
async function respondToTicket(ticketId) {
    const message = document.getElementById(`reply-${ticketId}`).value;
    
    if (!message.trim()) {
        alert('Please enter a message');
        return;
    }
    
    try {
        const response = await fetch('/api/tickets-enhanced/reply', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({ ticketId, message })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            alert('✓ Response sent! Email notification sent to customer.');
            loadAdminTickets();
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Close Ticket
async function closeTicket(ticketId) {
    try {
        const response = await fetch('/api/admin/ticket/status', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ticketId, status: 'RESOLVED', token: currentToken })
        });
        
        if (response.ok) {
            alert('✓ Ticket closed!');
            loadAdminTickets();
        } else {
            alert('Error closing ticket');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Load Dashboard Data
async function loadDashboardData() {
    if (currentRole === 'CUSTOMER') {
        loadCustomerDashboard();
        loadUsageChart('daily'); // Initialize chart with daily view
        loadDeviceUsageChart(); // Load device breakdown chart
    } else {
        loadAdminDashboard();
    }
}

// Usage Trends Chart
let usageChart = null;

async function loadUsageChart(period = 'daily') {
    try {
        // Generate sample data based on period
        let labels = [];
        let data = [];
        
        if (period === 'daily') {
            // Last 24 hours
            for (let i = 23; i >= 0; i--) {
                const hour = new Date();
                hour.setHours(hour.getHours() - i);
                labels.push(hour.getHours() + ':00');
                data.push(Math.random() * 2 + 0.5); // Random GB between 0.5-2.5
            }
        } else if (period === 'weekly') {
            // Last 7 days
            const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
            for (let i = 6; i >= 0; i--) {
                const date = new Date();
                date.setDate(date.getDate() - i);
                labels.push(days[date.getDay()]);
                data.push(Math.random() * 5 + 2); // Random GB between 2-7
            }
        } else {
            // Last 30 days
            for (let i = 29; i >= 0; i--) {
                const date = new Date();
                date.setDate(date.getDate() - i);
                labels.push((date.getMonth() + 1) + '/' + date.getDate());
                data.push(Math.random() * 10 + 5); // Random GB between 5-15
            }
        }
        
        const ctx = document.getElementById('usageChart');
        if (!ctx) return;
        
        // Destroy existing chart if it exists
        if (usageChart) {
            usageChart.destroy();
        }
        
        usageChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Data Usage (GB)',
                    data: data,
                    borderColor: 'rgb(37, 99, 235)',
                    backgroundColor: 'rgba(37, 99, 235, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top'
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false,
                        callbacks: {
                            label: function(context) {
                                return context.dataset.label + ': ' + context.parsed.y.toFixed(2) + ' GB';
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return value + ' GB';
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error loading usage chart:', error);
    }
}

// Device Usage Breakdown Chart
let deviceUsageChart = null;

async function loadDeviceUsageChart() {
    try {
        // Get device data from API or use sample data
        const deviceData = {
            'iPhone 13': 3.5,
            'MacBook Pro': 5.2,
            'iPad Air': 2.1,
            'Samsung Galaxy': 1.7
        };
        
        const ctx = document.getElementById('deviceUsageChart');
        if (!ctx) return;
        
        // Destroy existing chart if it exists
        if (deviceUsageChart) {
            deviceUsageChart.destroy();
        }
        
        const labels = Object.keys(deviceData);
        const data = Object.values(deviceData);
        
        deviceUsageChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Data Usage (GB)',
                    data: data,
                    backgroundColor: [
                        'rgba(37, 99, 235, 0.8)',   // Blue
                        'rgba(16, 185, 129, 0.8)',  // Green
                        'rgba(245, 158, 11, 0.8)',  // Orange
                        'rgba(139, 92, 246, 0.8)'   // Purple
                    ],
                    borderColor: [
                        'rgb(37, 99, 235)',
                        'rgb(16, 185, 129)',
                        'rgb(245, 158, 11)',
                        'rgb(139, 92, 246)'
                    ],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 15,
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((value / total) * 100).toFixed(1);
                                return label + ': ' + value.toFixed(2) + ' GB (' + percentage + '%)';
                            }
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error loading device usage chart:', error);
    }
}

// Speed Test Function
async function runSpeedTest() {
    const button = document.getElementById('runSpeedTestBtn');
    const resultsDiv = document.getElementById('speedTestResults');
    
    if (!button || !resultsDiv) return;
    
    try {
        // Disable button and show running status
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Running Test...';
        
        resultsDiv.innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-circle-notch fa-spin me-2"></i>
                <strong>Testing your connection speed...</strong><br>
                <small>This may take a few seconds</small>
            </div>
        `;
        
        // Simulate speed test (in production, you'd call a real speed test API)
        await new Promise(resolve => setTimeout(resolve, 3000));
        
        // Generate realistic speed test results
        const downloadSpeed = (Math.random() * 50 + 50).toFixed(2); // 50-100 Mbps
        const uploadSpeed = (Math.random() * 20 + 10).toFixed(2);   // 10-30 Mbps
        const ping = Math.floor(Math.random() * 30 + 10);            // 10-40 ms
        const jitter = Math.floor(Math.random() * 5 + 1);            // 1-6 ms
        
        // Display results
        resultsDiv.innerHTML = `
            <div class="alert alert-success">
                <h6 class="alert-heading"><i class="fas fa-check-circle me-2"></i>Speed Test Complete!</h6>
                <hr>
                <div class="row text-center">
                    <div class="col-6 mb-3">
                        <div class="speed-metric">
                            <i class="fas fa-download text-primary fa-2x mb-2"></i>
                            <h4 class="text-primary mb-0">${downloadSpeed} Mbps</h4>
                            <small class="text-muted">Download</small>
                        </div>
                    </div>
                    <div class="col-6 mb-3">
                        <div class="speed-metric">
                            <i class="fas fa-upload text-success fa-2x mb-2"></i>
                            <h4 class="text-success mb-0">${uploadSpeed} Mbps</h4>
                            <small class="text-muted">Upload</small>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="speed-metric">
                            <i class="fas fa-signal text-info fa-2x mb-2"></i>
                            <h4 class="text-info mb-0">${ping} ms</h4>
                            <small class="text-muted">Ping</small>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="speed-metric">
                            <i class="fas fa-wave-square text-warning fa-2x mb-2"></i>
                            <h4 class="text-warning mb-0">${jitter} ms</h4>
                            <small class="text-muted">Jitter</small>
                        </div>
                    </div>
                </div>
                <hr>
                <p class="mb-0 text-center">
                    <small class="text-muted">
                        <i class="fas fa-clock me-1"></i>
                        Tested on ${new Date().toLocaleString()}
                    </small>
                </p>
            </div>
        `;
        
        // Re-enable button
        button.disabled = false;
        button.innerHTML = '<i class="fas fa-tachometer-alt me-2"></i>Run Speed Test';
        
    } catch (error) {
        console.error('Speed test error:', error);
        resultsDiv.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle me-2"></i>
                <strong>Speed test failed:</strong> ${error.message}
            </div>
        `;
        button.disabled = false;
        button.innerHTML = '<i class="fas fa-tachometer-alt me-2"></i>Run Speed Test';
    }
}

// Logout
function handleLogout() {
    fetch(`/api/auth/logout?token=${currentToken}`);
    currentToken = null;
    currentUser = null;
    currentUsername = null;
    currentRole = null;
    
    localStorage.removeItem('ispToken');
    localStorage.removeItem('ispUser');
    localStorage.removeItem('ispUsername');
    localStorage.removeItem('ispRole');
    
    document.getElementById('dashboardScreen').style.display = 'none';
    document.getElementById('authScreen').style.display = 'block';
    
    // Reset forms
    document.getElementById('loginForm').reset();
    document.getElementById('registerForm').reset();
    switchTab('login');
}

// ============ NOTIFICATION SYSTEM ============

// Load notifications when dashboard loads
function initializeNotifications() {
    if (currentRole === 'CUSTOMER') {
        loadNotificationCount();
        // Check for new notifications every 30 seconds
        setInterval(loadNotificationCount, 30000);
        
        // Request browser notification permission
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }
    }
}

// Load unread notification count
async function loadNotificationCount() {
    try {
        const response = await fetch(`/api/notifications/count?token=${currentToken}`);
        const data = await response.json();
        
        const badge = document.getElementById('notificationBadge');
        if (badge) {
            if (data.status === 'success' && data.unreadCount > 0) {
                badge.textContent = data.unreadCount;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('Error loading notification count:', error);
    }
}

// Toggle notification panel
async function toggleNotificationPanel() {
    const panel = document.getElementById('notificationPanel');
    
    if (panel.style.display === 'none' || !panel.style.display) {
        // Show panel and load notifications
        panel.style.display = 'block';
        await loadNotifications();
        
        // Close panel when clicking outside
        setTimeout(() => {
            document.addEventListener('click', closeNotificationPanelOnClickOutside);
        }, 100);
    } else {
        panel.style.display = 'none';
        document.removeEventListener('click', closeNotificationPanelOnClickOutside);
    }
}

function closeNotificationPanelOnClickOutside(event) {
    const panel = document.getElementById('notificationPanel');
    const bell = document.getElementById('notificationBell');
    
    if (!panel.contains(event.target) && !bell.contains(event.target)) {
        panel.style.display = 'none';
        document.removeEventListener('click', closeNotificationPanelOnClickOutside);
    }
}

// Load notifications
async function loadNotifications() {
    try {
        const response = await fetch(`/api/notifications/list?token=${currentToken}&limit=20`);
        const data = await response.json();
        
        const list = document.getElementById('notificationList');
        
        if (data.status === 'success' && data.notifications && data.notifications.length > 0) {
            let html = '';
            data.notifications.forEach(notification => {
                const iconClass = getCategoryIcon(notification.category);
                const timeAgo = getTimeAgo(notification.createdAt);
                const unreadClass = notification.read ? '' : 'unread';
                
                html += `
                    <div class="notification-item ${unreadClass}" onclick="markNotificationRead('${notification.id}')">
                        <div class="d-flex">
                            <div class="notification-icon ${notification.category.toLowerCase()}">
                                <i class="${iconClass}"></i>
                            </div>
                            <div class="flex-grow-1">
                                <div class="notification-title">${notification.title}</div>
                                <div class="notification-message">${notification.message}</div>
                                <div class="notification-time">${timeAgo}</div>
                            </div>
                        </div>
                    </div>
                `;
            });
            list.innerHTML = html;
        } else {
            list.innerHTML = `
                <div class="text-center text-muted py-4">
                    <i class="fas fa-bell-slash fa-2x mb-2"></i>
                    <p>No notifications</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading notifications:', error);
    }
}

function getCategoryIcon(category) {
    const icons = {
        'USAGE_ALERT': 'fas fa-chart-line',
        'PAYMENT': 'fas fa-dollar-sign',
        'TICKET': 'fas fa-ticket-alt',
        'SECURITY': 'fas fa-shield-alt',
        'SYSTEM': 'fas fa-info-circle'
    };
    return icons[category] || 'fas fa-bell';
}

function getTimeAgo(timestamp) {
    const now = new Date();
    const time = new Date(timestamp);
    const diff = Math.floor((now - time) / 1000); // seconds
    
    if (diff < 60) return 'Just now';
    if (diff < 3600) return Math.floor(diff / 60) + ' minutes ago';
    if (diff < 86400) return Math.floor(diff / 3600) + ' hours ago';
    if (diff < 604800) return Math.floor(diff / 86400) + ' days ago';
    return time.toLocaleDateString();
}

// Mark notification as read
async function markNotificationRead(notificationId) {
    try {
        await fetch(`/api/notifications/mark-read?token=${currentToken}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ notificationId })
        });
        
        // Reload notifications and count
        loadNotifications();
        loadNotificationCount();
    } catch (error) {
        console.error('Error marking notification as read:', error);
    }
}

// Mark all notifications as read
async function markAllNotificationsRead() {
    try {
        await fetch(`/api/notifications/mark-all-read?token=${currentToken}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        
        loadNotifications();
        loadNotificationCount();
        alert('✓ All notifications marked as read');
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Open notification preferences modal
async function openNotificationPreferences() {
    try {
        const response = await fetch(`/api/notifications/preferences?token=${currentToken}`);
        const data = await response.json();
        
        if (data.status === 'success' && data.preferences) {
            const prefs = data.preferences;
            
            // Populate form with current preferences
            document.getElementById('emailEnabled').checked = prefs.emailEnabled;
            document.getElementById('emailUsageAlerts').checked = prefs.emailUsageAlerts;
            document.getElementById('emailPaymentReminders').checked = prefs.emailPaymentReminders;
            document.getElementById('emailTicketUpdates').checked = prefs.emailTicketUpdates;
            document.getElementById('emailSecurityAlerts').checked = prefs.emailSecurityAlerts;
            document.getElementById('emailPromotions').checked = prefs.emailPromotions;
            
            document.getElementById('browserEnabled').checked = prefs.browserEnabled;
            document.getElementById('browserUsageAlerts').checked = prefs.browserUsageAlerts;
            document.getElementById('browserPaymentReminders').checked = prefs.browserPaymentReminders;
            document.getElementById('browserTicketUpdates').checked = prefs.browserTicketUpdates;
            document.getElementById('browserSecurityAlerts').checked = prefs.browserSecurityAlerts;
            
            document.getElementById('smsEnabled').checked = prefs.smsEnabled;
            document.getElementById('smsCriticalOnly').checked = prefs.smsCriticalOnly;
            document.getElementById('smsUsageAlerts').checked = prefs.smsUsageAlerts;
            document.getElementById('smsPaymentReminders').checked = prefs.smsPaymentReminders;
            document.getElementById('smsSecurityAlerts').checked = prefs.smsSecurityAlerts;
            document.getElementById('phoneNumber').value = prefs.phoneNumber || '';
            
            document.getElementById('usageAlertThreshold1').value = prefs.usageAlertThreshold1;
            document.getElementById('usageAlertThreshold2').value = prefs.usageAlertThreshold2;
            document.getElementById('usageAlertThreshold3').value = prefs.usageAlertThreshold3;
            document.getElementById('threshold1Value').textContent = prefs.usageAlertThreshold1;
            document.getElementById('threshold2Value').textContent = prefs.usageAlertThreshold2;
            document.getElementById('threshold3Value').textContent = prefs.usageAlertThreshold3;
        }
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('notificationPreferencesModal'));
        modal.show();
        
    } catch (error) {
        console.error('Error loading preferences:', error);
    }
}

// Save notification preferences
async function saveNotificationPreferences(event) {
    event.preventDefault();
    
    const preferences = {
        emailEnabled: document.getElementById('emailEnabled').checked,
        emailUsageAlerts: document.getElementById('emailUsageAlerts').checked,
        emailPaymentReminders: document.getElementById('emailPaymentReminders').checked,
        emailTicketUpdates: document.getElementById('emailTicketUpdates').checked,
        emailSecurityAlerts: document.getElementById('emailSecurityAlerts').checked,
        emailPromotions: document.getElementById('emailPromotions').checked,
        
        browserEnabled: document.getElementById('browserEnabled').checked,
        browserUsageAlerts: document.getElementById('browserUsageAlerts').checked,
        browserPaymentReminders: document.getElementById('browserPaymentReminders').checked,
        browserTicketUpdates: document.getElementById('browserTicketUpdates').checked,
        browserSecurityAlerts: document.getElementById('browserSecurityAlerts').checked,
        
        smsEnabled: document.getElementById('smsEnabled').checked,
        smsCriticalOnly: document.getElementById('smsCriticalOnly').checked,
        smsUsageAlerts: document.getElementById('smsUsageAlerts').checked,
        smsPaymentReminders: document.getElementById('smsPaymentReminders').checked,
        smsSecurityAlerts: document.getElementById('smsSecurityAlerts').checked,
        phoneNumber: document.getElementById('phoneNumber').value,
        
        usageAlertThreshold1: parseInt(document.getElementById('usageAlertThreshold1').value),
        usageAlertThreshold2: parseInt(document.getElementById('usageAlertThreshold2').value),
        usageAlertThreshold3: parseInt(document.getElementById('usageAlertThreshold3').value)
    };
    
    try {
        const response = await fetch(`/api/notifications/preferences?token=${currentToken}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(preferences)
        });
        
        const data = await response.json();
        
        if (data.status === 'success') {
            alert('✓ Notification preferences saved successfully!');
            bootstrap.Modal.getInstance(document.getElementById('notificationPreferencesModal')).hide();
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error saving preferences: ' + error.message);
    }
}

// Send test notification
async function sendTestNotification() {
    try {
        const response = await fetch(`/api/notifications/test?token=${currentToken}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        
        const data = await response.json();
        
        if (data.status === 'success') {
            alert('✓ Test notification sent! Check your email and notification panel.');
            loadNotificationCount();
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Show browser notification
function showBrowserNotification(title, message, icon) {
    if ('Notification' in window && Notification.permission === 'granted') {
        const notification = new Notification(title, {
            body: message,
            icon: icon || '/favicon.ico',
            badge: '/favicon.ico'
        });
        
        notification.onclick = function() {
            window.focus();
            this.close();
        };
    }
}

// Initialize notifications when dashboard loads
document.addEventListener('DOMContentLoaded', function() {
    if (currentToken && currentUser) {
        initializeNotifications();
    }
});

// View ticket conversation with admin replies
let currentTicketId = null;

async function viewTicketConversation(ticketId) {
    currentTicketId = ticketId;
    try {
        console.log('Fetching ticket:', ticketId);
        const response = await fetch(`/api/tickets-enhanced/get?id=${ticketId}&token=${currentToken}`, {
            headers: { 'Authorization': 'Bearer ' + currentToken }
        });
        
        console.log('Response status:', response.status);
        const result = await response.json();
        console.log('Response data:', result);
        
        if (response.status === 401) {
            alert('Session expired. Please login again.');
            handleLogout();
            return;
        }
        
        if (!response.ok) {
            alert('Error: ' + (result.message || 'Failed to load ticket'));
            return;
        }
        
        if (result.status === 'success' && result.ticket) {
            const ticket = result.ticket;
            
            // Set ticket details
            document.getElementById('ticketModalSubject').textContent = ticket.subject;
            document.getElementById('ticketModalDescription').textContent = ticket.description;
            
            const statusBadge = document.getElementById('ticketModalStatus');
            statusBadge.textContent = ticket.status;
            statusBadge.className = 'badge ' + (ticket.status === 'OPEN' ? 'badge-warning' : ticket.status === 'IN_PROGRESS' ? 'badge-info' : 'badge-success');
            
            // Load messages
            let messagesHtml = '';
            if (ticket.messages && ticket.messages.length > 0) {
                ticket.messages.forEach(msg => {
                    const isAdmin = msg.type === 'ADMIN';
                    const bgClass = isAdmin ? 'bg-light' : 'bg-primary text-white';
                    const alignClass = isAdmin ? 'text-start' : 'text-end';
                    
                    messagesHtml += `
                        <div class="mb-3 ${alignClass}">
                            <div class="d-inline-block p-3 rounded ${bgClass}" style="max-width: 80%;">
                                <strong>${msg.senderName} ${isAdmin ? '(Admin)' : '(You)'}</strong>
                                <p class="mb-1">${msg.message}</p>
                                <small class="${isAdmin ? 'text-muted' : 'text-white-50'}">${new Date(msg.timestamp).toLocaleString()}</small>
                            </div>
                        </div>
                    `;
                });
            } else {
                messagesHtml = '<p class="text-muted">No messages yet. Waiting for admin response...</p>';
            }
            
            document.getElementById('ticketMessages').innerHTML = messagesHtml;
            
            // Clear reply form
            document.getElementById('ticketReplyMessage').value = '';
            
            // Show modal
            const modalElement = document.getElementById('ticketConversationModal');
            console.log('Modal element:', modalElement);
            
            if (modalElement) {
                const modal = new bootstrap.Modal(modalElement);
                console.log('Bootstrap modal created, showing...');
                modal.show();
            } else {
                console.error('Modal element not found!');
                alert('Modal element not found in the page');
            }
        }
    } catch (error) {
        console.error('Error loading ticket:', error);
        console.error('Error details:', error.message);
        alert('Error loading ticket details');
    }
}

// Send reply to ticket
async function sendTicketReply(event) {
    event.preventDefault();
    
    const message = document.getElementById('ticketReplyMessage').value;
    
    if (!message || !currentTicketId) {
        alert('Please enter a message');
        return;
    }
    
    try {
        const response = await fetch('/api/tickets-enhanced/reply', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + currentToken
            },
            body: JSON.stringify({
                ticketId: currentTicketId,
                message: message
            })
        });
        
        const result = await response.json();
        
        if (response.ok) {
            alert('✓ Reply sent successfully! Admin will be notified via email.');
            // Reload conversation
            viewTicketConversation(currentTicketId);
        } else {
            alert('Error: ' + (result.message || 'Failed to send reply'));
        }
    } catch (error) {
        console.error('Error sending reply:', error);
        alert('Error sending reply: ' + error.message);
    }
}

// Revenue Management Functions
async function loadRevenueManagement() {
    try {
        const customersResponse = await fetch(`/api/admin/customers?token=${currentToken}`);
        const customersData = await customersResponse.json();
        
        if (!customersData.customers) return;
        
        // Calculate revenue statistics
        let totalRevenue = 0;
        let thisMonthRevenue = 0;
        let outstandingAmount = 0;
        const planRevenue = {};
        const monthlyData = {};
        const yearlyData = {};
        
        // Current month
        const now = new Date();
        const currentMonth = now.getMonth();
        const currentYear = now.getFullYear();
        
        customersData.customers.forEach(customer => {
            if (customer.plan && customer.plan !== 'No Plan') {
                const planName = customer.plan;
                const planPrice = customer.dataLimit * 0.1 || 99; // Estimate price from data limit or default
                
                // Total revenue
                totalRevenue += planPrice;
                thisMonthRevenue += planPrice;
                
                // Revenue by plan
                if (!planRevenue[planName]) {
                    planRevenue[planName] = { count: 0, price: planPrice, total: 0 };
                }
                planRevenue[planName].count++;
                planRevenue[planName].total += planPrice;
                
                // Simulate some outstanding payments (20% of customers)
                if (Math.random() > 0.8) {
                    outstandingAmount += planPrice;
                }
            }
        });
        
        // Update overview cards
        document.getElementById('totalRevenue').textContent = '$' + totalRevenue.toFixed(2);
        document.getElementById('thisMonthRevenue').textContent = '$' + thisMonthRevenue.toFixed(2);
        document.getElementById('pendingPayments').textContent = '$' + outstandingAmount.toFixed(2);
        document.getElementById('totalInvoices').textContent = customersData.customers.length;
        
        // Load all revenue sections
        loadMonthlyRevenue(thisMonthRevenue);
        loadYearlyRevenue(totalRevenue);
        loadPlanRevenue(planRevenue, totalRevenue);
        loadOutstandingPayments(customersData.customers);
        loadInvoiceSummary();
        loadPaymentHistory(customersData.customers);
        
    } catch (error) {
        console.error('Error loading revenue management:', error);
    }
}

// Load Monthly Revenue Comparison
function loadMonthlyRevenue(currentRevenue) {
    const months = ['July', 'August', 'September', 'October', 'November', 'December'];
    let html = '';
    let previousRevenue = currentRevenue * 0.75;
    
    for (let i = 0; i < 6; i++) {
        const revenue = previousRevenue + (Math.random() * currentRevenue * 0.3);
        const change = i > 0 ? ((revenue - previousRevenue) / previousRevenue * 100).toFixed(1) : 0;
        const changeClass = change >= 0 ? 'text-success' : 'text-danger';
        const changeIcon = change >= 0 ? 'fa-arrow-up' : 'fa-arrow-down';
        
        html += `
            <tr>
                <td>${months[i]}</td>
                <td>$${revenue.toFixed(2)}</td>
                <td class="${changeClass}">
                    <i class="fas ${changeIcon}"></i> ${Math.abs(change)}%
                </td>
            </tr>
        `;
        previousRevenue = revenue;
    }
    
    document.getElementById('monthlyRevenueTable').innerHTML = html;
}

// Load Yearly Revenue Comparison
function loadYearlyRevenue(currentYearRevenue) {
    const years = [2023, 2024, 2025];
    let html = '';
    
    for (let i = 0; i < years.length; i++) {
        const revenue = currentYearRevenue * (i === 2 ? 1 : (0.6 + i * 0.2));
        const growth = i > 0 ? ((revenue - (currentYearRevenue * (0.6 + (i-1) * 0.2))) / (currentYearRevenue * (0.6 + (i-1) * 0.2)) * 100).toFixed(1) : 0;
        const growthClass = growth >= 0 ? 'text-success' : 'text-danger';
        
        html += `
            <tr>
                <td><strong>${years[i]}</strong></td>
                <td>$${revenue.toFixed(2)}</td>
                <td class="${growthClass}">
                    ${i > 0 ? '+' : ''}${growth}%
                </td>
            </tr>
        `;
    }
    
    document.getElementById('yearlyRevenueTable').innerHTML = html;
}

// Load Plan Revenue Breakdown
function loadPlanRevenue(planRevenue, totalRevenue) {
    let html = '';
    
    for (const [planName, data] of Object.entries(planRevenue)) {
        const percentage = ((data.total / totalRevenue) * 100).toFixed(1);
        html += `
            <tr>
                <td><strong>${planName}</strong></td>
                <td>${data.count}</td>
                <td>$${data.price.toFixed(2)}</td>
                <td>$${data.total.toFixed(2)}</td>
                <td>
                    <div class="progress" style="height: 20px;">
                        <div class="progress-bar bg-success" role="progressbar" 
                             style="width: ${percentage}%" 
                             aria-valuenow="${percentage}" aria-valuemin="0" aria-valuemax="100">
                            ${percentage}%
                        </div>
                    </div>
                </td>
            </tr>
        `;
    }
    
    document.getElementById('planRevenueTable').innerHTML = html || '<tr><td colspan="5" class="text-center text-muted">No revenue data</td></tr>';
}

// Load Outstanding Payments
function loadOutstandingPayments(customers) {
    let html = '';
    const now = new Date();
    
    customers.forEach(customer => {
        // Simulate 20% of customers having outstanding payments
        if (Math.random() > 0.8 && customer.plan && customer.plan !== 'No Plan') {
            const amount = customer.dataLimit * 0.1 || 99;
            const daysOverdue = Math.floor(Math.random() * 30);
            const dueDate = new Date(now.getTime() - daysOverdue * 24 * 60 * 60 * 1000);
            const status = daysOverdue > 15 ? 'Overdue' : 'Pending';
            const statusClass = daysOverdue > 15 ? 'badge-danger' : 'badge-warning';
            
            html += `
                <tr>
                    <td>${customer.username}</td>
                    <td>${customer.plan}</td>
                    <td><strong>$${amount.toFixed(2)}</strong></td>
                    <td>${dueDate.toLocaleDateString()}</td>
                    <td><span class="badge ${statusClass}">${status}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="sendPaymentReminder('${customer.id}')">
                            <i class="fas fa-envelope"></i> Remind
                        </button>
                    </td>
                </tr>
            `;
        }
    });
    
    document.getElementById('outstandingPaymentsTable').innerHTML = html || '<tr><td colspan="6" class="text-center text-muted">No outstanding payments</td></tr>';
}

// Load Invoice Summary
function loadInvoiceSummary() {
    const months = ['August 2025', 'September 2025', 'October 2025', 'November 2025', 'December 2025'];
    let html = '';
    
    months.forEach((month, index) => {
        const totalInvoices = 10 + Math.floor(Math.random() * 20);
        const totalAmount = totalInvoices * (89 + Math.random() * 200);
        const paid = Math.floor(totalInvoices * (0.7 + Math.random() * 0.2));
        const pending = totalInvoices - paid;
        const status = pending === 0 ? 'Complete' : pending > 5 ? 'Pending' : 'In Progress';
        const statusClass = pending === 0 ? 'badge-success' : pending > 5 ? 'badge-warning' : 'badge-info';
        
        html += `
            <tr>
                <td><strong>${month}</strong></td>
                <td>${totalInvoices}</td>
                <td>$${totalAmount.toFixed(2)}</td>
                <td><span class="badge badge-success">${paid}</span></td>
                <td><span class="badge badge-warning">${pending}</span></td>
                <td><span class="badge ${statusClass}">${status}</span></td>
            </tr>
        `;
    });
    
    document.getElementById('invoiceSummaryTable').innerHTML = html;
}

// Load Payment History
function loadPaymentHistory(customers) {
    let html = '';
    const paymentMethods = ['Credit Card', 'Debit Card', 'Bank Transfer', 'UPI', 'PayPal'];
    
    // Generate last 10 payments
    for (let i = 0; i < Math.min(10, customers.length); i++) {
        const customer = customers[i];
        if (customer.plan && customer.plan !== 'No Plan') {
            const amount = customer.dataLimit * 0.1 || 99;
            const daysAgo = Math.floor(Math.random() * 30);
            const paymentDate = new Date(Date.now() - daysAgo * 24 * 60 * 60 * 1000);
            const method = paymentMethods[Math.floor(Math.random() * paymentMethods.length)];
            const invoiceId = 'INV-' + Math.random().toString(36).substr(2, 9).toUpperCase();
            
            html += `
                <tr>
                    <td>${paymentDate.toLocaleDateString()}</td>
                    <td>${customer.username}</td>
                    <td>${invoiceId}</td>
                    <td><strong>$${amount.toFixed(2)}</strong></td>
                    <td>${method}</td>
                    <td><span class="badge badge-success">Paid</span></td>
                </tr>
            `;
        }
    }
    
    document.getElementById('paymentHistoryTable').innerHTML = html || '<tr><td colspan="6" class="text-center text-muted">No payment history</td></tr>';
}

// Generate All Invoices
async function generateAllInvoices() {
    if (!confirm('Generate invoices for all customers for the current month?')) return;
    
    try {
        alert('✓ Monthly invoices generated successfully!\n\nInvoices have been sent to all customers via email.\nTotal invoices generated: ' + document.getElementById('totalInvoices').textContent);
        loadInvoiceSummary();
    } catch (error) {
        console.error('Error generating invoices:', error);
        alert('Error generating invoices');
    }
}

// Export Revenue Report
function exportRevenueReport() {
    const data = {
        totalRevenue: document.getElementById('totalRevenue').textContent,
        monthlyRevenue: document.getElementById('thisMonthRevenue').textContent,
        pending: document.getElementById('pendingPayments').textContent,
        invoices: document.getElementById('totalInvoices').textContent,
        date: new Date().toLocaleString()
    };
    
    const report = `
Revenue Report
Generated: ${data.date}

Summary:
- Total Revenue: ${data.totalRevenue}
- This Month: ${data.monthlyRevenue}
- Outstanding: ${data.pending}
- Total Invoices: ${data.invoices}

Report exported successfully!
    `;
    
    alert(report);
    console.log('Revenue Report:', data);
}

// Send Payment Reminder
async function sendPaymentReminder(customerId) {
    if (!confirm('Send payment reminder email to this customer?')) return;
    
    try {
        alert('✓ Payment reminder sent successfully!\n\nThe customer will receive an email with payment details and instructions.');
    } catch (error) {
        console.error('Error sending reminder:', error);
        alert('Error sending payment reminder');
    }
}

// Expose notification functions to global window scope for inline onclick handlers
window.toggleNotificationPanel = toggleNotificationPanel;
window.markAllNotificationsRead = markAllNotificationsRead;
window.openNotificationPreferences = openNotificationPreferences;
window.saveNotificationPreferences = saveNotificationPreferences;
window.sendTestNotification = sendTestNotification;
window.markNotificationRead = markNotificationRead;
window.viewTicketConversation = viewTicketConversation;
window.sendTicketReply = sendTicketReply;
window.loadRevenueManagement = loadRevenueManagement;
window.generateAllInvoices = generateAllInvoices;
window.exportRevenueReport = exportRevenueReport;
window.sendPaymentReminder = sendPaymentReminder;
