package com.isp.web;

import com.isp.model.Customer;
import com.isp.model.Ticket;
import com.isp.model.DeviceConnection;
import com.isp.repo.CustomerRepository;
import com.isp.repo.TicketRepository;
import com.isp.repo.UsageRepository;
import com.isp.repo.DeviceConnectionRepository;
import com.isp.service.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// PDFBox (for invoice generation)
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * Minimal HTTP server (no external deps) exposing REST-like endpoints for the ISP system.
 * This is a lightweight alternative to full Spring Boot, suitable for demo purposes.
 */
public class WebServer {
    private final CustomerService customerService;
    private final UsageService usageService;
    private final TicketService ticketService;
    private final HotspotService hotspotService;
    private final BillingService billingService;
    private com.sun.net.httpserver.HttpServer httpServer;
    private final java.util.Map<String,String> sessions = new java.util.concurrent.ConcurrentHashMap<>(); // token -> userId
    private final java.util.Map<String,String> otpStore = new java.util.concurrent.ConcurrentHashMap<>(); // email -> OTP
    private final java.util.Map<String,Long> otpExpiry = new java.util.concurrent.ConcurrentHashMap<>(); // email -> expiry time
    private final com.isp.service.UserService userService;
    private final com.isp.service.CustomerProfileService profileService;
    private final com.isp.service.DataPlanService dataPlanService;
    private final com.isp.service.TicketEnhancedService ticketEnhancedService;
    private final com.isp.service.EmailService emailService;
    private final com.isp.service.NotificationService notificationService;
    private com.isp.repo.UsageAlertRepository usageAlertRepo;
    private com.isp.repo.DailyUsageRepository dailyUsageRepo;
    private com.isp.repo.SpeedTestRepository speedTestRepo;

    public WebServer() {
        // Initialize database schema first
        com.isp.util.DatabaseConnection.initializeSchema();
        
        // Wire repositories and services
        CustomerRepository customerRepo = new CustomerRepository();
        UsageRepository usageRepo = new UsageRepository();
        TicketRepository ticketRepo = new TicketRepository();
        DeviceConnectionRepository deviceRepo = new DeviceConnectionRepository();
        this.customerService = new CustomerService(customerRepo);
        this.usageService = new UsageService(usageRepo);
        this.ticketService = new TicketService(ticketRepo);
        this.hotspotService = new HotspotService(deviceRepo);
        this.billingService = new BillingService(customerRepo, usageRepo);
        // Additional services for auth/profiles/plans
        this.emailService = new com.isp.service.EmailService();
        com.isp.repo.UserRepository userRepo = new com.isp.repo.UserRepository();
        this.userService = new com.isp.service.UserService(userRepo, emailService);
        com.isp.repo.CustomerProfileRepository profileRepo = new com.isp.repo.CustomerProfileRepository();
        this.profileService = new com.isp.service.CustomerProfileService(profileRepo, emailService);
        com.isp.repo.DataPlanRepository planRepo = new com.isp.repo.DataPlanRepository();
        this.dataPlanService = new com.isp.service.DataPlanService(planRepo);
        this.dataPlanService.initializeDefaultPlans();
        // Initialize TicketEnhancedService
        com.isp.repo.TicketEnhancedRepository ticketEnhancedRepo = new com.isp.repo.TicketEnhancedRepository();
        this.ticketEnhancedService = new com.isp.service.TicketEnhancedService(ticketEnhancedRepo, emailService);

        // Initialize NotificationService
        com.isp.util.DatabaseConnection dbConn = new com.isp.util.DatabaseConnection();
        com.isp.repo.NotificationRepository notificationRepo = new com.isp.repo.NotificationRepository(dbConn);
        com.isp.repo.NotificationPreferencesRepository preferencesRepo = new com.isp.repo.NotificationPreferencesRepository(dbConn);
        com.isp.service.SmsService smsService = new com.isp.service.SmsService();
        this.notificationService = new com.isp.service.NotificationService(notificationRepo, preferencesRepo, userRepo, emailService, smsService);

        // Seed demo users with real email addresses
        try {
            // Check if admin exists first
            if (userRepo.findByUsername("admin").isEmpty()) {
                com.isp.model.User admin = this.userService.registerUser("admin", "muthuvel04041971@gmail.com", "admin123", com.isp.model.User.Role.ADMIN);
                System.out.println("Seeded admin user: " + admin.getUsername() + " (" + admin.getEmail() + ")");
            } else {
                System.out.println("Admin user already exists in database");
            }
        } catch (Exception e) {
            System.err.println("Failed to seed admin user: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            // Check if customer exists first
            if (userRepo.findByUsername("customer").isEmpty()) {
                com.isp.model.User cust = this.userService.registerUser("customer", "vaishnavimuthuvel223@gmail.com", "customer123", com.isp.model.User.Role.CUSTOMER);
                // Create profile for demo customer
                this.profileService.createProfile(cust.getId(), "Vaishnavi Muthuvel", cust.getEmail());
                System.out.println("Seeded demo customer: " + cust.getUsername() + " (" + cust.getEmail() + ")");
            } else {
                System.out.println("Customer user already exists in database");
            }
        } catch (Exception e) {
            System.err.println("Failed to seed customer user: " + e.getMessage());
            e.printStackTrace();
        }

        // Initialize sample device data
        initializeSampleDevices(deviceRepo, userRepo);
        
        // Initialize repositories for usage monitoring
        this.usageAlertRepo = new com.isp.repo.UsageAlertRepository();
        this.dailyUsageRepo = new com.isp.repo.DailyUsageRepository();
        this.speedTestRepo = new com.isp.repo.SpeedTestRepository();
        
        // Initialize sample usage data
        initializeSampleUsageData(userRepo);
    }

    private void initializeSampleDevices(DeviceConnectionRepository deviceRepo, com.isp.repo.UserRepository userRepo) {
        try {
            // Find the demo customer (muthukrishx or customer)
            java.util.Optional<com.isp.model.User> customerUserOpt = userRepo.findByUsername("muthukrishx");
            if (customerUserOpt.isEmpty()) {
                customerUserOpt = userRepo.findByUsername("customer");
            }
            
            if (customerUserOpt.isEmpty()) {
                System.out.println("No customer found for sample device data");
                return;
            }

            String userId = customerUserOpt.get().getId();
            java.util.Optional<com.isp.model.CustomerProfile> profileOpt = profileService.findByUserId(userId);
            
            if (profileOpt.isEmpty()) {
                System.out.println("No customer profile found for sample device data");
                return;
            }

            String customerId = profileOpt.get().getId();
            
            // Check if devices already exist
            if (!deviceRepo.findByCustomerId(customerId).isEmpty()) {
                System.out.println("Sample device data already exists");
                return;
            }

            // Create 3 sample devices with varied data
            String[] deviceNames = {"iQOO Z7", "OPPO A57", "Samsung M14"};
            String[] macAddresses = {"AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:03"};
            String[] ipAddresses = {"192.168.1.101", "192.168.1.102", "192.168.1.103"};
            boolean[] activeStatus = {true, false, true};
            double[] dataUsageMB = {1250.5, 89.3, 2450.8};  // Between 50-2500 MB
            double[] speeds = {45.2, 38.7, 52.3};  // Average speeds in Mbps

            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 0; i < 3; i++) {
                String deviceId = java.util.UUID.randomUUID().toString();
                LocalDateTime connectTime = now.minusHours(i * 2 + 1);
                LocalDateTime disconnectTime = activeStatus[i] ? null : connectTime.plusMinutes(35 + i * 10);
                
                com.isp.model.DeviceConnection device = new com.isp.model.DeviceConnection(
                    deviceId,
                    customerId,
                    deviceNames[i],
                    macAddresses[i],
                    connectTime,
                    disconnectTime,
                    dataUsageMB[i] / 1024.0,  // Convert MB to GB for internal storage
                    activeStatus[i],
                    ipAddresses[i],
                    speeds[i]
                );
                
                deviceRepo.save(device);
                System.out.println("Created sample device: " + deviceNames[i] + " - " + 
                                 (activeStatus[i] ? "Active" : "Disconnected") + 
                                 " - " + String.format("%.2f MB", dataUsageMB[i]));
            }
            
            System.out.println("Sample device data initialized successfully!");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize sample device data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeSampleUsageData(com.isp.repo.UserRepository userRepo) {
        try {
            // Find the demo customer
            java.util.Optional<com.isp.model.User> customerUserOpt = userRepo.findByUsername("customer");
            if (customerUserOpt.isEmpty()) {
                customerUserOpt = userRepo.findByUsername("muthukrishx");
            }
            
            if (customerUserOpt.isEmpty()) {
                System.out.println("No customer found for sample usage data");
                return;
            }

            String userId = customerUserOpt.get().getId();
            java.util.Optional<com.isp.model.CustomerProfile> profileOpt = profileService.findByUserId(userId);
            
            if (profileOpt.isEmpty()) {
                System.out.println("No customer profile found for sample usage data");
                return;
            }

            String customerId = profileOpt.get().getId();
            
            // Check if usage data already exists
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            if (!dailyUsageRepo.findByCustomerIdAndDateRange(customerId, thirtyDaysAgo, LocalDateTime.now()).isEmpty()) {
                System.out.println("Sample usage data already exists");
                return;
            }

            // Create 30 days of usage data
            LocalDateTime now = LocalDateTime.now();
            java.util.Random random = new java.util.Random();
            
            for (int i = 29; i >= 0; i--) {
                LocalDateTime date = now.minusDays(i);
                
                // Generate realistic usage data
                double baseUsage = 0.5 + (random.nextDouble() * 2.5);  // 0.5 to 3 GB
                double downloadGB = baseUsage * (0.7 + random.nextDouble() * 0.2);  // 70-90% download
                double uploadGB = baseUsage - downloadGB;  // Rest is upload
                double peakSpeed = 30.0 + (random.nextDouble() * 40.0);  // 30-70 Mbps
                int devices = 2 + random.nextInt(3);  // 2-4 devices
                
                com.isp.model.DailyUsage usage = new com.isp.model.DailyUsage(
                    java.util.UUID.randomUUID().toString(),
                    customerId,
                    date,
                    baseUsage,
                    uploadGB,
                    downloadGB,
                    peakSpeed,
                    devices
                );
                
                dailyUsageRepo.save(usage);
            }
            
            System.out.println("Sample usage data initialized successfully (30 days)!");
            
            // Create a speed test result
            com.isp.model.SpeedTest speedTest = new com.isp.model.SpeedTest(
                java.util.UUID.randomUUID().toString(),
                customerId,
                LocalDateTime.now(),
                45.8 + (random.nextDouble() * 20),  // 45-65 Mbps download
                8.5 + (random.nextDouble() * 5),    // 8-13 Mbps upload
                15 + random.nextInt(20)             // 15-35 ms ping
            );
            speedTestRepo.save(speedTest);
            System.out.println("Sample speed test created");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize sample usage data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        WebServer server = new WebServer();
        server.start(port);
        System.out.println("Web server started on http://localhost:" + port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer = server;
        System.out.println("[WebServer] Binding HttpServer to port: " + port);

    // Static UI at root: serve files from src/main/resources/public
    server.createContext("/", new StaticHandler("/public", "index.html"));

        server.createContext("/api/health", exchange -> ok(exchange, json("status", "ok")));

        // Customers
        server.createContext("/api/customers", new CustomersHandler());
        // Usage (record & query)
        server.createContext("/api/usage", new UsageHandler());
        // Tickets
        server.createContext("/api/tickets", new TicketsHandler());
        // Hotspot
        server.createContext("/api/hotspot", new HotspotHandler());
        // Billing
        server.createContext("/api/billing", new BillingHandler());
        
        // Notifications
        server.createContext("/api/notifications", new com.isp.web.NotificationHandler(notificationService, sessions));

        // Auth endpoints
        server.createContext("/api/auth/register", exchange -> {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String username = json.get("username").getAsString();
                String email = json.get("email").getAsString();
                String password = json.get("password").getAsString();
                String roleStr = json.has("role") ? json.get("role").getAsString() : "CUSTOMER";
                com.isp.model.User.Role role = "ADMIN".equalsIgnoreCase(roleStr) ? com.isp.model.User.Role.ADMIN : com.isp.model.User.Role.CUSTOMER;
                try {
                    com.isp.model.User u = userService.registerUser(username, email, password, role);
                    if (role == com.isp.model.User.Role.CUSTOMER) profileService.createProfile(u.getId(), username, email);
                    setJson(exchange);
                    com.google.gson.JsonObject _r1 = new com.google.gson.JsonObject();
                    _r1.addProperty("status","success");
                    _r1.addProperty("userId", u.getId());
                    send(exchange,200, _r1.toString());
                } catch (RuntimeException re) {
                    setJson(exchange);
                    com.google.gson.JsonObject _r2 = new com.google.gson.JsonObject();
                    _r2.addProperty("status","error");
                    _r2.addProperty("message", re.getMessage());
                    send(exchange,400, _r2.toString());
                }
            } catch (Exception e) {
                setJson(exchange);
                com.google.gson.JsonObject _r3 = new com.google.gson.JsonObject();
                _r3.addProperty("status","error");
                _r3.addProperty("message","Invalid request");
                send(exchange,400, _r3.toString());
            }
        });

        server.createContext("/api/auth/login", exchange -> {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String username = json.get("username").getAsString();
                String password = json.get("password").getAsString();
                java.util.Optional<com.isp.model.User> opt = userService.authenticate(username, password);
                setJson(exchange);
                if (opt.isPresent()) {
                    com.isp.model.User u = opt.get();
                    String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString((u.getId()+":"+System.currentTimeMillis()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    sessions.put(token, u.getId());
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("status","success"); resp.addProperty("token", token); resp.addProperty("username", u.getUsername()); resp.addProperty("role", u.getRole().name()); resp.addProperty("userId", u.getId());
                    send(exchange,200, resp.toString());
                } else {
                    com.google.gson.JsonObject _r4 = new com.google.gson.JsonObject();
                    _r4.addProperty("status","error");
                    _r4.addProperty("message","Invalid credentials");
                    send(exchange,401, _r4.toString());
                }
            } catch (Exception e) {
                setJson(exchange);
                com.google.gson.JsonObject _r5 = new com.google.gson.JsonObject();
                _r5.addProperty("status","error");
                _r5.addProperty("message","Invalid request");
                send(exchange,400, _r5.toString());
            }
        });

        server.createContext("/api/auth/logout", exchange -> {
            String token = null;
            java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of());
            if (!auth.isEmpty()) {
                String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim();
            }
            if (token==null) {
                String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; }
            }
            if (token!=null) sessions.remove(token);
            setJson(exchange);
            com.google.gson.JsonObject _r6 = new com.google.gson.JsonObject();
            _r6.addProperty("status","success");
            send(exchange,200, _r6.toString());
        });

        // Forgot Password - Request OTP
        server.createContext("/api/auth/forgot-password", exchange -> {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String email = json.get("email").getAsString();
                
                // Check if user exists
                java.util.Optional<com.isp.model.User> userOpt = userService.findByEmail(email);
                setJson(exchange);
                
                if (userOpt.isEmpty()) {
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("status","error");
                    resp.addProperty("message","No account found with this email address");
                    send(exchange,404, resp.toString());
                    return;
                }
                
                // Generate 6-digit OTP
                String otp = String.format("%06d", new java.util.Random().nextInt(999999));
                otpStore.put(email, otp);
                otpExpiry.put(email, System.currentTimeMillis() + 600000); // 10 minutes expiry
                
                // Send OTP via email
                String subject = "Password Reset OTP - ISP Management";
                String message = String.format(
                    "Hello %s,\n\n" +
                    "Your OTP for password reset is: %s\n\n" +
                    "This OTP will expire in 10 minutes.\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Best regards,\nISP Management Team",
                    userOpt.get().getUsername(), otp
                );
                
                emailService.sendEmail(email, subject, message);
                System.out.println("[OTP] Generated OTP for " + email + ": " + otp);
                
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("status","success");
                resp.addProperty("message","OTP sent to your email");
                send(exchange,200, resp.toString());
                
            } catch (Exception e) {
                System.err.println("[FORGOT PASSWORD] Error: " + e.getMessage());
                e.printStackTrace();
                setJson(exchange);
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("status","error");
                resp.addProperty("message","Failed to send OTP: " + e.getMessage());
                send(exchange,500, resp.toString());
            }
        });

        // Reset Password - Verify OTP and Update Password
        server.createContext("/api/auth/reset-password", exchange -> {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String email = json.get("email").getAsString();
                String otp = json.get("otp").getAsString();
                String newPassword = json.get("newPassword").getAsString();
                
                setJson(exchange);
                
                // Check if OTP exists and is valid
                if (!otpStore.containsKey(email)) {
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("status","error");
                    resp.addProperty("message","No OTP found. Please request a new OTP.");
                    send(exchange,400, resp.toString());
                    return;
                }
                
                // Check OTP expiry
                Long expiry = otpExpiry.get(email);
                if (expiry == null || System.currentTimeMillis() > expiry) {
                    otpStore.remove(email);
                    otpExpiry.remove(email);
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("status","error");
                    resp.addProperty("message","OTP has expired. Please request a new OTP.");
                    send(exchange,400, resp.toString());
                    return;
                }
                
                // Verify OTP
                String storedOtp = otpStore.get(email);
                if (!otp.equals(storedOtp)) {
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("status","error");
                    resp.addProperty("message","Invalid OTP. Please try again.");
                    send(exchange,400, resp.toString());
                    return;
                }
                
                // Update password
                boolean updated = userService.resetPassword(email, newPassword);
                
                if (updated) {
                    // Clear OTP
                    otpStore.remove(email);
                    otpExpiry.remove(email);
                    
                    System.out.println("[PASSWORD RESET] Password updated successfully for: " + email);
                    
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("status","success");
                    resp.addProperty("message","Password reset successful");
                    send(exchange,200, resp.toString());
                } else {
                    com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                    resp.addProperty("status","error");
                    resp.addProperty("message","Failed to update password");
                    send(exchange,500, resp.toString());
                }
                
            } catch (Exception e) {
                System.err.println("[RESET PASSWORD] Error: " + e.getMessage());
                e.printStackTrace();
                setJson(exchange);
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("status","error");
                resp.addProperty("message","Failed to reset password: " + e.getMessage());
                send(exchange,500, resp.toString());
            }
        });

        // Customer profile / plans
        server.createContext("/api/customer/profile", exchange -> {
            String token = null;
            java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of());
            if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r7 = new com.google.gson.JsonObject(); _r7.addProperty("status","error"); _r7.addProperty("message","Unauthorized"); send(exchange,401, _r7.toString()); return; }
            java.util.Optional<com.isp.model.CustomerProfile> prof = profileService.findByUserId(userId);
            com.google.gson.JsonObject resp = new com.google.gson.JsonObject(); resp.addProperty("status","success");
            if (prof.isPresent()) {
                com.isp.model.CustomerProfile p = prof.get(); com.google.gson.JsonObject j = new com.google.gson.JsonObject();
                j.addProperty("customerId", p.getId()); j.addProperty("fullName", p.getFullName());
                if (p.getCurrentPlan()!=null) { j.addProperty("currentPlan", p.getCurrentPlan().getName()); j.addProperty("pricePerMonth", p.getCurrentPlan().getPricePerMonth()); }
                resp.add("profile", j);
            } else resp.add("profile", null);
            setJson(exchange); send(exchange,200, resp.toString());
        });

        server.createContext("/api/customer/plans", exchange -> {
            java.util.Collection<com.isp.model.DataPlan> plans = dataPlanService.listAll();
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (com.isp.model.DataPlan p : plans) {
                com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                o.addProperty("id", p.getId()); o.addProperty("name", p.getName()); o.addProperty("dataGB", p.getDataGB()); o.addProperty("pricePerMonth", p.getPricePerMonth()); o.addProperty("description", p.getDescription());
                arr.add(o);
            }
            com.google.gson.JsonObject resp = new com.google.gson.JsonObject(); resp.addProperty("status","success"); resp.add("plans", arr);
            setJson(exchange); send(exchange,200, resp.toString());
        });

        server.createContext("/api/customer/select-plan", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
            String token = null; java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r11 = new com.google.gson.JsonObject(); _r11.addProperty("status","error"); _r11.addProperty("message","Unauthorized"); send(exchange,401, _r11.toString()); return; }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                System.out.println("[PLAN SELECTION] Request received for user: " + userId);
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String planId = json.get("planId").getAsString();
                System.out.println("[PLAN SELECTION] Plan ID: " + planId);
                java.util.Optional<com.isp.model.DataPlan> planOpt = dataPlanService.findById(planId);
                if (planOpt.isEmpty()) { System.err.println("[PLAN SELECTION] ERROR: Plan not found"); setJson(exchange); com.google.gson.JsonObject _r8 = new com.google.gson.JsonObject(); _r8.addProperty("status","error"); _r8.addProperty("message","Plan not found"); send(exchange,404, _r8.toString()); return; }
                java.util.Optional<com.isp.model.User> uOpt = userService.findById(userId);
                String email = uOpt.map(com.isp.model.User::getEmail).orElse("");
                String uname = uOpt.map(com.isp.model.User::getUsername).orElse("");
                System.out.println("[PLAN SELECTION] Customer: " + uname + " (" + email + ")");
                java.util.Optional<com.isp.model.CustomerProfile> profOpt = profileService.findByUserId(userId);
                if (profOpt.isEmpty()) { System.err.println("[PLAN SELECTION] ERROR: Profile not found"); setJson(exchange); com.google.gson.JsonObject _r12 = new com.google.gson.JsonObject(); _r12.addProperty("status","error"); _r12.addProperty("message","Profile not found"); send(exchange,404, _r12.toString()); return; }
                System.out.println("[PLAN SELECTION] Calling selectPlan service...");
                profileService.selectPlan(profOpt.get().getId(), planOpt.get(), email, uname);
                System.out.println("[PLAN SELECTION] Plan selection completed successfully");
                setJson(exchange); com.google.gson.JsonObject _r9 = new com.google.gson.JsonObject(); _r9.addProperty("status","success"); _r9.addProperty("message","Plan selected and confirmation email sent!"); send(exchange,200, _r9.toString());
            } catch (Exception e) { System.err.println("[PLAN SELECTION] ERROR: " + e.getMessage()); e.printStackTrace(); setJson(exchange); com.google.gson.JsonObject _r10 = new com.google.gson.JsonObject(); _r10.addProperty("status","error"); _r10.addProperty("message","Invalid request: " + e.getMessage()); send(exchange,400, _r10.toString()); }
        });

        // Customer devices dashboard
        server.createContext("/api/customer/devices", exchange -> {
            String token = null; java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r13 = new com.google.gson.JsonObject(); _r13.addProperty("status","error"); _r13.addProperty("message","Unauthorized"); send(exchange,401, _r13.toString()); return; }
            
            // Get customer profile to find customerId
            java.util.Optional<com.isp.model.CustomerProfile> profOpt = profileService.findByUserId(userId);
            if (profOpt.isEmpty()) { setJson(exchange); com.google.gson.JsonObject _r14 = new com.google.gson.JsonObject(); _r14.addProperty("status","error"); _r14.addProperty("message","Profile not found"); send(exchange,404, _r14.toString()); return; }
            
            String customerId = profOpt.get().getId();
            // Get device history for this customer
            java.util.List<DeviceConnection> devices = hotspotService.getDeviceHistory(customerId);
            
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (DeviceConnection d : devices) {
                com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                o.addProperty("device_id", d.getId());
                o.addProperty("device_name", d.getDeviceName());
                o.addProperty("customer_id", d.getCustomerId());
                o.addProperty("connection_start_time", d.getConnectTime().format(formatter));
                if (d.getDisconnectTime() != null) {
                    o.addProperty("connection_end_time", d.getDisconnectTime().format(formatter));
                } else {
                    o.addProperty("connection_end_time", "Still Active");
                }
                o.addProperty("total_data_used_mb", String.format("%.2f", d.getDataUsedGB() * 1024));
                o.addProperty("average_speed_mbps", String.format("%.2f", d.getAverageSpeedMbps()));
                o.addProperty("ip_address", d.getIpAddress());
                o.addProperty("status", d.isActive() ? "Active" : "Disconnected");
                arr.add(o);
            }
            
            com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
            resp.addProperty("status","success");
            resp.add("devices", arr);
            setJson(exchange); send(exchange,200, resp.toString());
        });

        // Enhanced tickets with messaging
        server.createContext("/api/tickets-enhanced/create", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
            String token = null; java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            System.out.println("[TICKET CREATE] Token received: " + (token != null ? "Yes" : "No"));
            String userId = token==null?null:sessions.get(token);
            System.out.println("[TICKET CREATE] User ID from session: " + (userId != null ? userId : "null - session expired or invalid"));
            System.out.println("[TICKET CREATE] Active sessions count: " + sessions.size());
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r15 = new com.google.gson.JsonObject(); _r15.addProperty("status","error"); _r15.addProperty("message","Unauthorized - Session expired. Please logout and login again."); send(exchange,401, _r15.toString()); return; }
            
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String subject = json.get("subject").getAsString();
                String description = json.get("description").getAsString();
                
                java.util.Optional<com.isp.model.User> uOpt = userService.findById(userId);
                if (uOpt.isEmpty()) { setJson(exchange); com.google.gson.JsonObject _r16 = new com.google.gson.JsonObject(); _r16.addProperty("status","error"); _r16.addProperty("message","User not found"); send(exchange,404, _r16.toString()); return; }
                
                com.isp.model.User user = uOpt.get();
                com.isp.model.TicketEnhanced ticket = ticketEnhancedService.createTicket(userId, user.getUsername(), subject, description, user.getEmail());
                
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("status","success");
                resp.addProperty("ticketId", ticket.getId());
                resp.addProperty("message", "Ticket created and notification sent to admin");
                setJson(exchange); send(exchange,200, resp.toString());
            } catch (Exception e) { setJson(exchange); com.google.gson.JsonObject _r17 = new com.google.gson.JsonObject(); _r17.addProperty("status","error"); _r17.addProperty("message","Invalid request: " + e.getMessage()); send(exchange,400, _r17.toString()); }
        });

        server.createContext("/api/tickets-enhanced/reply", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
            String token = null; java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r18 = new com.google.gson.JsonObject(); _r18.addProperty("status","error"); _r18.addProperty("message","Unauthorized"); send(exchange,401, _r18.toString()); return; }
            
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String ticketId = json.get("ticketId").getAsString();
                String message = json.get("message").getAsString();
                
                java.util.Optional<com.isp.model.User> uOpt = userService.findById(userId);
                if (uOpt.isEmpty()) { setJson(exchange); com.google.gson.JsonObject _r19 = new com.google.gson.JsonObject(); _r19.addProperty("status","error"); _r19.addProperty("message","User not found"); send(exchange,404, _r19.toString()); return; }
                
                com.isp.model.User user = uOpt.get();
                java.util.Optional<com.isp.model.TicketEnhanced> ticketOpt = ticketEnhancedService.findById(ticketId);
                if (ticketOpt.isEmpty()) { setJson(exchange); com.google.gson.JsonObject _r20 = new com.google.gson.JsonObject(); _r20.addProperty("status","error"); _r20.addProperty("message","Ticket not found"); send(exchange,404, _r20.toString()); return; }
                
                com.isp.model.TicketEnhanced ticket = ticketOpt.get();
                String messageType = user.getRole() == com.isp.model.User.Role.ADMIN ? "ADMIN" : "CUSTOMER";
                String recipientEmail = messageType.equals("ADMIN") ? ticket.getCustomerName() : "muthuvel04041971@gmail.com";
                
                // Get customer email if admin is replying
                if (messageType.equals("ADMIN")) {
                    java.util.Optional<com.isp.model.User> custOpt = userService.findById(ticket.getCustomerId());
                    if (custOpt.isPresent()) {
                        recipientEmail = custOpt.get().getEmail();
                    }
                }
                
                ticketEnhancedService.addMessage(ticketId, userId, user.getUsername(), message, messageType, user.getEmail(), recipientEmail);
                
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("status","success");
                resp.addProperty("message", messageType.equals("ADMIN") ? "Reply sent and ticket marked as in progress" : "Message sent to admin");
                setJson(exchange); send(exchange,200, resp.toString());
            } catch (Exception e) { setJson(exchange); com.google.gson.JsonObject _r21 = new com.google.gson.JsonObject(); _r21.addProperty("status","error"); _r21.addProperty("message","Invalid request: " + e.getMessage()); send(exchange,400, _r21.toString()); }
        });

        server.createContext("/api/tickets-enhanced/list", exchange -> {
            String token = null; java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r22 = new com.google.gson.JsonObject(); _r22.addProperty("status","error"); _r22.addProperty("message","Unauthorized"); send(exchange,401, _r22.toString()); return; }
            
            java.util.Optional<com.isp.model.User> uOpt = userService.findById(userId);
            if (uOpt.isEmpty()) { setJson(exchange); com.google.gson.JsonObject _r23 = new com.google.gson.JsonObject(); _r23.addProperty("status","error"); _r23.addProperty("message","User not found"); send(exchange,404, _r23.toString()); return; }
            
            com.isp.model.User user = uOpt.get();
            java.util.List<com.isp.model.TicketEnhanced> tickets;
            if (user.getRole() == com.isp.model.User.Role.ADMIN) {
                tickets = ticketEnhancedService.listAll();
            } else {
                tickets = ticketEnhancedService.findByCustomerId(userId);
            }
            
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (com.isp.model.TicketEnhanced t : tickets) {
                com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                o.addProperty("id", t.getId());
                o.addProperty("subject", t.getSubject());
                o.addProperty("description", t.getDescription());
                o.addProperty("status", t.getStatus().name());
                o.addProperty("customerName", t.getCustomerName());
                o.addProperty("createdAt", t.getCreatedAt().toString());
                o.addProperty("messageCount", t.getMessages().size());
                arr.add(o);
            }
            
            com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
            resp.addProperty("status","success");
            resp.add("tickets", arr);
            setJson(exchange); send(exchange,200, resp.toString());
        });

        server.createContext("/api/tickets-enhanced/get", exchange -> {
            String token = null; 
            String ticketId = null;
            
            // Parse query parameters
            String q = exchange.getRequestURI().getQuery(); 
            if (q!=null) {
                for (String p: q.split("&")) { 
                    String[] kv = p.split("=",2); 
                    if (kv.length==2 && kv[0].equals("token")) token = kv[1]; 
                    if (kv.length==2 && kv[0].equals("id")) ticketId = kv[1]; 
                } 
            }
            
            // Check Authorization header
            java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); 
            if (!auth.isEmpty()) { 
                String v = auth.get(0); 
                if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); 
            }
            
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r = new com.google.gson.JsonObject(); _r.addProperty("status","error"); _r.addProperty("message","Unauthorized"); send(exchange,401, _r.toString()); return; }
            
            if (ticketId == null) { setJson(exchange); com.google.gson.JsonObject _r1 = new com.google.gson.JsonObject(); _r1.addProperty("status","error"); _r1.addProperty("message","Ticket ID required"); send(exchange,400, _r1.toString()); return; }
            
            java.util.Optional<com.isp.model.TicketEnhanced> ticketOpt = ticketEnhancedService.findById(ticketId);
            if (ticketOpt.isEmpty()) { setJson(exchange); com.google.gson.JsonObject _r2 = new com.google.gson.JsonObject(); _r2.addProperty("status","error"); _r2.addProperty("message","Ticket not found"); send(exchange,404, _r2.toString()); return; }
            
            com.isp.model.TicketEnhanced ticket = ticketOpt.get();
            
            com.google.gson.JsonObject ticketObj = new com.google.gson.JsonObject();
            ticketObj.addProperty("id", ticket.getId());
            ticketObj.addProperty("subject", ticket.getSubject());
            ticketObj.addProperty("description", ticket.getDescription());
            ticketObj.addProperty("status", ticket.getStatus().name());
            ticketObj.addProperty("customerName", ticket.getCustomerName());
            ticketObj.addProperty("createdAt", ticket.getCreatedAt().toString());
            
            com.google.gson.JsonArray messagesArr = new com.google.gson.JsonArray();
            for (com.isp.model.TicketMessage msg : ticket.getMessages()) {
                com.google.gson.JsonObject msgObj = new com.google.gson.JsonObject();
                msgObj.addProperty("id", msg.getId());
                msgObj.addProperty("senderName", msg.getSenderName());
                msgObj.addProperty("message", msg.getMessage());
                msgObj.addProperty("type", msg.getType());
                msgObj.addProperty("timestamp", msg.getSentAt().toString());
                messagesArr.add(msgObj);
            }
            ticketObj.add("messages", messagesArr);
            
            com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
            resp.addProperty("status","success");
            resp.add("ticket", ticketObj);
            setJson(exchange); send(exchange,200, resp.toString());
        });

        // Admin: List all customers with their profiles
        server.createContext("/api/admin/customers", exchange -> {
            String token = null; java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r24 = new com.google.gson.JsonObject(); _r24.addProperty("status","error"); _r24.addProperty("message","Unauthorized"); send(exchange,401, _r24.toString()); return; }
            
            java.util.Optional<com.isp.model.User> uOpt = userService.findById(userId);
            if (uOpt.isEmpty() || uOpt.get().getRole() != com.isp.model.User.Role.ADMIN) { 
                setJson(exchange); com.google.gson.JsonObject _r25 = new com.google.gson.JsonObject(); 
                _r25.addProperty("status","error"); 
                _r25.addProperty("message","Admin access required"); 
                send(exchange,403, _r25.toString()); 
                return; 
            }
            
            // Get all users with CUSTOMER role
            java.util.Collection<com.isp.model.User> allUsers = userService.listAll();
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            
            for (com.isp.model.User user : allUsers) {
                if (user.getRole() == com.isp.model.User.Role.CUSTOMER) {
                    com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                    o.addProperty("id", user.getId());
                    o.addProperty("username", user.getUsername());
                    o.addProperty("email", user.getEmail());
                    o.addProperty("status", user.getStatus().name());
                    
                    // Get customer profile if exists
                    java.util.Optional<com.isp.model.CustomerProfile> profileOpt = profileService.findByUserId(user.getId());
                    if (profileOpt.isPresent()) {
                        com.isp.model.CustomerProfile profile = profileOpt.get();
                        o.addProperty("fullName", profile.getFullName());
                        
                        if (profile.getCurrentPlan() != null) {
                            o.addProperty("plan", profile.getCurrentPlan().getName());
                            o.addProperty("dataLimit", profile.getCurrentPlan().getDataGB());
                        } else {
                            o.addProperty("plan", "No Plan");
                            o.addProperty("dataLimit", 0);
                        }
                        
                        // TODO: Add actual data usage when implemented
                        o.addProperty("dataUsed", 0);
                    } else {
                        o.addProperty("fullName", "N/A");
                        o.addProperty("plan", "No Plan");
                        o.addProperty("dataLimit", 0);
                        o.addProperty("dataUsed", 0);
                    }
                    
                    arr.add(o);
                }
            }
            
            com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
            resp.addProperty("status","success");
            resp.add("customers", arr);
            setJson(exchange); send(exchange,200, resp.toString());
        });

        // Customer Detail endpoint
        server.createContext("/api/admin/customer-detail", exchange -> {
            String token = null; java.util.List<String> auth = exchange.getRequestHeaders().getOrDefault("Authorization", java.util.List.of()); if (!auth.isEmpty()) { String v = auth.get(0); if (v.toLowerCase().startsWith("bearer ")) token = v.substring(7).trim(); }
            if (token==null) { String q = exchange.getRequestURI().getQuery(); if (q!=null) for (String p: q.split("&")) { String[] kv = p.split("=",2); if (kv.length==2 && kv[0].equals("token")) token = kv[1]; } }
            String userId = token==null?null:sessions.get(token);
            if (userId==null) { setJson(exchange); com.google.gson.JsonObject _r = new com.google.gson.JsonObject(); _r.addProperty("status","error"); _r.addProperty("message","Unauthorized"); send(exchange,401, _r.toString()); return; }
            
            java.util.Optional<com.isp.model.User> uOpt = userService.findById(userId);
            if (uOpt.isEmpty() || uOpt.get().getRole() != com.isp.model.User.Role.ADMIN) { 
                setJson(exchange); com.google.gson.JsonObject _r = new com.google.gson.JsonObject(); 
                _r.addProperty("status","error"); 
                _r.addProperty("message","Admin access required"); 
                send(exchange,403, _r.toString()); 
                return; 
            }
            
            // Get customerId from query
            String customerId = null;
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && kv[0].equals("customerId")) {
                        customerId = kv[1];
                    }
                }
            }
            
            if (customerId == null) {
                setJson(exchange); 
                com.google.gson.JsonObject _r = new com.google.gson.JsonObject(); 
                _r.addProperty("status","error"); 
                _r.addProperty("message","Customer ID required"); 
                send(exchange,400, _r.toString()); 
                return;
            }
            
            // Get customer user
            java.util.Optional<com.isp.model.User> customerOpt = userService.findById(customerId);
            if (customerOpt.isEmpty()) {
                setJson(exchange); 
                com.google.gson.JsonObject _r = new com.google.gson.JsonObject(); 
                _r.addProperty("status","error"); 
                _r.addProperty("message","Customer not found"); 
                send(exchange,404, _r.toString()); 
                return;
            }
            
            com.isp.model.User customer = customerOpt.get();
            com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
            resp.addProperty("id", customer.getId());
            resp.addProperty("username", customer.getUsername());
            resp.addProperty("email", customer.getEmail());
            resp.addProperty("status", customer.getStatus().name());
            
            // Get customer profile
            java.util.Optional<com.isp.model.CustomerProfile> profileOpt = profileService.findByUserId(customer.getId());
            if (profileOpt.isPresent()) {
                com.isp.model.CustomerProfile profile = profileOpt.get();
                resp.addProperty("fullName", profile.getFullName());
                
                if (profile.getCurrentPlan() != null) {
                    com.google.gson.JsonObject planObj = new com.google.gson.JsonObject();
                    planObj.addProperty("name", profile.getCurrentPlan().getName());
                    planObj.addProperty("dataGB", profile.getCurrentPlan().getDataGB());
                    planObj.addProperty("price", profile.getCurrentPlan().getPricePerMonth());
                    planObj.addProperty("description", profile.getCurrentPlan().getDescription());
                    resp.add("plan", planObj);
                }
                
                if (profile.getPlanStartDate() != null) {
                    resp.addProperty("planStartDate", profile.getPlanStartDate().toString());
                }
                if (profile.getPlanRenewalDate() != null) {
                    resp.addProperty("planRenewalDate", profile.getPlanRenewalDate().toString());
                }
            }
            
            resp.addProperty("status", "success");
            setJson(exchange); 
            send(exchange, 200, resp.toString());
        });

        server.setExecutor(null);
        server.start();
        System.out.println("[WebServer] HttpServer started and listening on port: " + port);
    }

    // Handlers
    private static class StaticHandler implements HttpHandler {
        private final String resourceBase;
        private final String indexFile;

        StaticHandler(String resourceBase, String indexFile) {
            this.resourceBase = resourceBase;
            this.indexFile = indexFile;
        }

        @Override public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) { // safety: do not serve API through static handler
                notFound(ex, msg("Unknown path"));
                return;
            }
            String resPath = resourceBase + ("/".equals(path) ? "/" + indexFile : path);
            if (resPath.endsWith("/")) {
                resPath += indexFile;
            }
            try (InputStream is = WebServer.class.getResourceAsStream(resPath)) {
                if (is == null) {
                    notFound(ex, msg("Not found"));
                    return;
                }
                byte[] bytes = is.readAllBytes();
                Headers h = ex.getResponseHeaders();
                h.set("Content-Type", contentTypeFromPath(resPath));
                ex.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    private class CustomersHandler implements HttpHandler {
        private final Pattern byId = Pattern.compile("^/api/customers/([^/]+)$");

        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byId.matcher(path);
                if (m.matches()) {
                    String id = m.group(1);
                    var c = customerService.findById(id);
                    if (c.isPresent()) ok(ex, customerToJson(c.get())); else notFound(ex, msg("Customer not found"));
                } else if ("/api/customers".equals(path)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    boolean first = true;
                    for (Customer c : customerService.listAll()) {
                        if (!first) sb.append(',');
                        sb.append(customerToJson(c));
                        first = false;
                    }
                    sb.append("]");
                    ok(ex, sb.toString());
                } else {
                    notFound(ex, msg("Unknown path"));
                }
            } else if ("POST".equalsIgnoreCase(method) && "/api/customers".equals(path)) {
                Map<String, String> form = readForm(ex);
                String name = form.get("name");
                String email = form.get("email");
                if (isBlank(name) || isBlank(email)) { badRequest(ex, msg("name and email required")); return; }
                Customer c = customerService.createCustomer(name, email);
                created(ex, customerToJson(c));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                Matcher m = byId.matcher(path);
                if (m.matches()) {
                    customerService.deleteCustomer(m.group(1));
                    ok(ex, msg("deleted"));
                } else badRequest(ex, msg("invalid customer id"));
            } else {
                methodNotAllowed(ex);
            }
        }
    }

    private class UsageHandler implements HttpHandler {
        private final Pattern byCustomer = Pattern.compile("^/api/usage/([^/]+)$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/usage".equals(path)) {
                Map<String, String> form = readForm(ex);
                String customerId = form.get("customerId");
                String gbStr = form.get("gigabytes");
                if (isBlank(customerId) || isBlank(gbStr)) { badRequest(ex, msg("customerId and gigabytes required")); return; }
                try {
                    double gb = Double.parseDouble(gbStr);
                    var u = usageService.recordUsage(customerId, gb);
                    ok(ex, "{\"id\":\""+u.getId()+"\",\"customerId\":\""+u.getCustomerId()+"\",\"gigabytes\":"+u.getGigabytes()+"}");
                } catch (NumberFormatException nfe) {
                    badRequest(ex, msg("gigabytes must be a number"));
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byCustomer.matcher(path);
                if (m.matches()) {
                    String customerId = m.group(1);
                    var list = usageService.getUsageForCustomer(customerId);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i=0;i<list.size();i++) {
                        var u = list.get(i);
                        if (i>0) sb.append(',');
                        sb.append("{\"id\":\"").append(u.getId()).append("\",\"customerId\":\"").append(u.getCustomerId()).append("\",\"gigabytes\":").append(u.getGigabytes()).append("}");
                    }
                    sb.append("]");
                    ok(ex, sb.toString());
                } else badRequest(ex, msg("invalid path"));
            } else methodNotAllowed(ex);
        }
    }

    private class TicketsHandler implements HttpHandler {
        private final Pattern byCustomer = Pattern.compile("^/api/tickets/customer/([^/]+)$");
        private final Pattern byIdStatus = Pattern.compile("^/api/tickets/([^/]+)/status$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/tickets".equals(path)) {
                Map<String,String> form = readForm(ex);
                String customerId = form.get("customerId");
                String description = form.get("description");
                if (isBlank(customerId) || isBlank(description)) { badRequest(ex, msg("customerId and description required")); return; }
                var t = ticketService.createTicket(customerId, description);
                ok(ex, "{\"id\":\""+t.getId()+"\",\"status\":\""+t.getStatus()+"\"}");
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byCustomer.matcher(path);
                if (m.matches()) {
                    String customerId = m.group(1);
                    var list = ticketService.findByCustomer(customerId);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i=0;i<list.size();i++) {
                        var t = list.get(i);
                        if (i>0) sb.append(',');
                        sb.append("{\"id\":\"").append(t.getId()).append("\",\"status\":\"").append(t.getStatus()).append("\",\"description\":\"").append(escape(t.getDescription())).append("\"}");
                    }
                    sb.append("]");
                    ok(ex, sb.toString());
                } else badRequest(ex, msg("invalid path"));
            } else if (("POST".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
                Matcher m = byIdStatus.matcher(path);
                if (m.matches()) {
                    Map<String,String> form = readForm(ex);
                    String st = form.get("status");
                    try {
                        Ticket.Status status = Ticket.Status.valueOf(st);
                        ticketService.updateTicketStatus(m.group(1), status);
                        ok(ex, msg("updated"));
                    } catch (Exception e) {
                        badRequest(ex, msg("invalid status"));
                    }
                } else methodNotAllowed(ex);
            } else methodNotAllowed(ex);
        }
    }

    private class HotspotHandler implements HttpHandler {
        private final Pattern customerHistory = Pattern.compile("^/api/hotspot/customer/([^/]+)/history$");
        private final Pattern customerActive = Pattern.compile("^/api/hotspot/customer/([^/]+)/active$");
        private final Pattern allActive = Pattern.compile("^/api/hotspot/active$");
        private final Pattern usageUpdate = Pattern.compile("^/api/hotspot/([^/]+)/usage$");
        private final Pattern disconnect = Pattern.compile("^/api/hotspot/([^/]+)/disconnect$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/hotspot/connect".equals(path)) {
                Map<String,String> form = readForm(ex);
                String customerId = form.get("customerId");
                String deviceName = form.get("deviceName");
                String mac = form.get("macAddress");
                if (isBlank(customerId) || isBlank(deviceName) || isBlank(mac)) { badRequest(ex, msg("customerId, deviceName, macAddress required")); return; }
                var dc = hotspotService.connectDevice(customerId, deviceName, mac);
                ok(ex, "{\"id\":\""+dc.getId()+"\",\"deviceName\":\""+escape(dc.getDeviceName())+"\"}");
            } else if ("POST".equalsIgnoreCase(method)) {
                Matcher mu = usageUpdate.matcher(path);
                Matcher md = disconnect.matcher(path);
                if (mu.matches()) {
                    Map<String,String> form = readForm(ex);
                    String add = form.get("additionalDataGB");
                    try {
                        double gb = Double.parseDouble(add);
                        hotspotService.updateDeviceUsage(mu.group(1), gb);
                        ok(ex, msg("updated"));
                    } catch (Exception e) { badRequest(ex, msg("invalid additionalDataGB")); }
                } else if (md.matches()) {
                    Map<String,String> form = readForm(ex);
                    String total = form.get("finalDataUsedGB");
                    try {
                        double gb = Double.parseDouble(total);
                        hotspotService.disconnectDevice(md.group(1), gb);
                        ok(ex, msg("disconnected"));
                    } catch (Exception e) { badRequest(ex, msg("invalid finalDataUsedGB")); }
                } else { methodNotAllowed(ex); }
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher mh = customerHistory.matcher(path);
                Matcher ma = customerActive.matcher(path);
                Matcher maa = allActive.matcher(path);
                if (mh.matches()) {
                    var list = hotspotService.getDeviceHistory(mh.group(1));
                    ok(ex, deviceListToJson(list));
                } else if (ma.matches()) {
                    var list = hotspotService.getActiveDevices(ma.group(1));
                    ok(ex, deviceListToJson(list));
                } else if (maa.matches()) {
                    var list = hotspotService.getAllActiveDevices();
                    ok(ex, deviceListToJson(list));
                } else { badRequest(ex, msg("invalid path")); }
            } else methodNotAllowed(ex);
        }
    }

    private class BillingHandler implements HttpHandler {
        private final Pattern byCustomer = Pattern.compile("^/api/billing/customer/([^/]+)/summary$");
        private final Pattern invoiceByCustomer = Pattern.compile("^/api/billing/customer/([^/]+)/invoice$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/billing/run".equals(path)) {
                billingService.runBillingCycle();
                ok(ex, msg("billing run"));
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byCustomer.matcher(path);
                if (m.matches()) {
                    String customerId = m.group(1);
                    ok(ex, json("summary", billingService.getBillingSummary(customerId)));
                } else {
                    Matcher inv = invoiceByCustomer.matcher(path);
                    if (inv.matches()) {
                        String customerId = inv.group(1);
                        byte[] pdf = generateInvoicePdf(customerId);
                        if (pdf == null) { notFound(ex, msg("customer not found")); return; }
                        Headers h = ex.getResponseHeaders();
                        h.set("Content-Type", "application/pdf");
                        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
                        h.set("Content-Disposition", "attachment; filename=invoice-"+customerId+"-"+ts+".pdf");
                        ex.sendResponseHeaders(200, pdf.length);
                        try (OutputStream os = ex.getResponseBody()) { os.write(pdf); }
                    } else badRequest(ex, msg("invalid path"));
                }
            } else methodNotAllowed(ex);
        }
    }

    // Helpers
    private static void setJson(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            try { ex.sendResponseHeaders(204, -1); } catch (IOException ignored) {}
        }
    }

    private static void ok(HttpExchange ex, String body) throws IOException { send(ex, 200, body); }
    private static void created(HttpExchange ex, String body) throws IOException { send(ex, 201, body); }
    private static void badRequest(HttpExchange ex, String body) throws IOException { send(ex, 400, body); }
    private static void notFound(HttpExchange ex, String body) throws IOException { send(ex, 404, body); }
    private static void methodNotAllowed(HttpExchange ex) throws IOException { send(ex, 405, msg("method not allowed")); }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void sendHtml(HttpExchange ex, int status, String html) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "text/html; charset=utf-8");
        send(ex, status, html);
    }

    private static String contentTypeFromPath(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html")) return "text/html; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".ico")) return "image/x-icon";
        if (p.endsWith(".json")) return "application/json; charset=utf-8";
        return "text/plain; charset=utf-8";
    }

    private static Map<String,String> readForm(HttpExchange ex) throws IOException {
        String body;
        try (InputStream is = ex.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String,String> map = new HashMap<>();
        if (!body.isEmpty()) {
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                String k = urlDecode(kv[0]);
                String v = kv.length>1 ? urlDecode(kv[1]) : "";
                map.put(k, v);
            }
        }
        return map;
    }

    private static String urlDecode(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
    private static String isJson(String s) { return s == null ? "null" : ("\""+escape(s)+"\""); }
    private static String json(String k, String v) { return "{\""+k+"\":"+isJson(v)+"}"; }
    private static String msg(String m) { return json("message", m); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static String customerToJson(Customer c) {
        return "{\"id\":\""+c.getId()+"\",\"name\":\""+escape(c.getName())+"\",\"email\":\""+escape(c.getEmail())+"\"}";
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<list.size();i++) {
            if (i>0) sb.append(',');
            sb.append("\"").append(escape(String.valueOf(list.get(i)))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String deviceListToJson(List<DeviceConnection> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<list.size();i++) {
            DeviceConnection d = list.get(i);
            if (i>0) sb.append(',');
            sb.append("{\"id\":\"").append(d.getId()).append("\",")
              .append("\"customerId\":\"").append(d.getCustomerId()).append("\",")
              .append("\"deviceName\":\"").append(escape(d.getDeviceName())).append("\",")
              .append("\"macAddress\":\"").append(escape(d.getMacAddress())).append("\",")
              .append("\"dataUsedGB\":").append(String.format(Locale.ROOT, "%.3f", d.getDataUsedGB())).append(',')
              .append("\"active\":").append(d.isActive()).append(',')
              .append("\"connectedMinutes\":").append(d.getConnectionDurationMinutes())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private byte[] generateInvoicePdf(String customerId) {
        // Get user and profile
        System.out.println("[INVOICE] Generating invoice for customer ID: " + customerId);
        Optional<com.isp.model.User> userOpt = userService.findById(customerId);
        if (userOpt.isEmpty()) {
            System.err.println("[INVOICE] User not found: " + customerId);
            return null;
        }
        com.isp.model.User user = userOpt.get();
        System.out.println("[INVOICE] User found: " + user.getUsername());
        
        Optional<com.isp.model.CustomerProfile> profileOpt = profileService.findByUserId(customerId);
        if (profileOpt.isEmpty()) {
            System.err.println("[INVOICE] Profile not found for user: " + customerId);
            return null;
        }
        com.isp.model.CustomerProfile profile = profileOpt.get();
        System.out.println("[INVOICE] Profile found, plan: " + (profile.getCurrentPlan() != null ? profile.getCurrentPlan().getName() : "No plan"));
        
        String issued = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String invoiceNumber = "INV-" + customerId.substring(0, 8) + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                // Header blue bar
                cs.setNonStrokingColor(33, 150, 243);
                cs.addRect(0, y - 20, page.getMediaBox().getWidth(), 20);
                cs.fill();
                y -= 40;

                // Title
                cs.beginText();
                cs.setNonStrokingColor(0);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                cs.newLineAtOffset(margin, y);
                cs.showText("INVOICE");
                cs.endText();

                y -= 30;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Invoice #: " + invoiceNumber);
                cs.endText();

                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Issue Date: " + issued);
                cs.endText();

                y -= 30;
                // Customer block
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Bill To:");
                cs.endText();

                y -= 18;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText(profile.getFullName());
                cs.endText();
                
                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText(user.getEmail());
                cs.endText();

                y -= 30;
                // Plan Details
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Service Details:");
                cs.endText();

                y -= 20;
                if (profile.getCurrentPlan() != null) {
                    com.isp.model.DataPlan plan = profile.getCurrentPlan();
                    
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Plan: " + plan.getName());
                    cs.endText();
                    
                    y -= 14;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Data Allowance: " + plan.getDataGB() + " GB");
                    cs.endText();
                    
                    y -= 14;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Description: " + plan.getDescription());
                    cs.endText();
                    
                    y -= 14;
                    if (profile.getPlanStartDate() != null) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Plan Start Date: " + profile.getPlanStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        cs.endText();
                        y -= 14;
                    }
                    
                    if (profile.getPlanRenewalDate() != null) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Next Renewal Date: " + profile.getPlanRenewalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        cs.endText();
                        y -= 14;
                    }
                    
                    // Amount
                    y -= 30;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(String.format(Locale.ROOT, "Amount Due: $%.2f", plan.getPricePerMonth()));
                    cs.endText();
                } else {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("No active plan");
                    cs.endText();
                    
                    y -= 30;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Amount Due: $0.00");
                    cs.endText();
                }
                
                // Footer
                y -= 50;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.setNonStrokingColor(100, 100, 100);
                cs.newLineAtOffset(margin, y);
                cs.showText("Thank you for choosing our ISP services!");
                cs.endText();
                
                y -= 12;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.newLineAtOffset(margin, y);
                cs.showText("For support, contact: support@isp.com | Phone: 1-800-ISP-HELP");
                cs.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
