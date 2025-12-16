package com.isp;

import com.isp.model.Customer;
import com.isp.model.DeviceConnection;
import com.isp.model.NetworkUsage;
import com.isp.model.Ticket;
import com.isp.repo.CustomerRepository;
import com.isp.repo.DeviceConnectionRepository;
import com.isp.repo.TicketRepository;
import com.isp.repo.UsageRepository;
import com.isp.service.*;

import java.util.Scanner;

/**
 * Main CLI application for ISP Management System.
 */
public class Main {
    public static void main(String[] args) {
        // Initialize repositories
        CustomerRepository customerRepo = new CustomerRepository();
        UsageRepository usageRepo = new UsageRepository();
        TicketRepository ticketRepo = new TicketRepository();
        DeviceConnectionRepository deviceRepo = new DeviceConnectionRepository();

        // Initialize services
        CustomerService customerService = new CustomerService(customerRepo);
        UsageService usageService = new UsageService(usageRepo);
        TicketService ticketService = new TicketService(ticketRepo);
        HotspotService hotspotService = new HotspotService(deviceRepo);
        BillingService billingService = new BillingService(customerRepo, usageRepo);
        Scheduler scheduler = new Scheduler(billingService);

        // Schedule automated billing every 120 seconds (for demo)
        scheduler.scheduleBilling(120, 120);

        // CLI Loop
        Scanner scanner = new Scanner(System.in);
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   ISP Management System - CLI Demo   ║");
        System.out.println("╚═══════════════════════════════════════╝");

        boolean running = true;
        while (running) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        createCustomer(scanner, customerService);
                        break;
                    case "2":
                        recordUsage(scanner, usageService);
                        break;
                    case "3":
                        createTicket(scanner, ticketService);
                        break;
                    case "4":
                        listCustomers(customerService);
                        break;
                    case "5":
                        viewCustomerDetails(scanner, customerService, usageService, ticketService, hotspotService, billingService);
                        break;
                    case "6":
                        listTickets(ticketService);
                        break;
                    case "7":
                        updateTicketStatus(scanner, ticketService);
                        break;
                    case "8":
                        billingService.runBillingCycle();
                        break;
                    case "9":
                        hotspotMenu(scanner, hotspotService);
                        break;
                    case "0":
                        System.out.println("\nShutting down...");
                        scheduler.shutdown();
                        running = false;
                        break;
                    default:
                        System.out.println("❌ Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("Goodbye!");
    }

    private static void displayMenu() {
        System.out.println("\n┌─────────────────────────────────────┐");
        System.out.println("│            MAIN MENU                │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 1. Create Customer                  │");
        System.out.println("│ 2. Record Network Usage             │");
        System.out.println("│ 3. Create Support Ticket            │");
        System.out.println("│ 4. List All Customers               │");
        System.out.println("│ 5. View Customer Details            │");
        System.out.println("│ 6. List All Tickets                 │");
        System.out.println("│ 7. Update Ticket Status             │");
        System.out.println("│ 8. Run Billing Cycle Now            │");
        System.out.println("│ 9. Hotspot Management               │");
        System.out.println("│ 0. Exit                             │");
        System.out.println("└─────────────────────────────────────┘");
        System.out.print("Enter your choice: ");
    }

    private static void createCustomer(Scanner scanner, CustomerService service) {
        System.out.print("Enter customer name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter customer email: ");
        String email = scanner.nextLine().trim();

        if (name.isEmpty() || email.isEmpty()) {
            System.out.println("❌ Name and email cannot be empty.");
            return;
        }

        Customer customer = service.createCustomer(name, email);
        System.out.println("✓ Customer created successfully!");
        System.out.println("  ID: " + customer.getId());
    }

    private static void recordUsage(Scanner scanner, UsageService service) {
        System.out.print("Enter customer ID: ");
        String customerId = scanner.nextLine().trim();
        System.out.print("Enter data usage in GB: ");
        String gbStr = scanner.nextLine().trim();

        try {
            double gb = Double.parseDouble(gbStr);
            if (gb <= 0) {
                System.out.println("❌ Usage must be positive.");
                return;
            }
            service.recordUsage(customerId, gb);
            System.out.println("✓ Usage recorded successfully!");
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format.");
        }
    }

    private static void createTicket(Scanner scanner, TicketService service) {
        System.out.print("Enter customer ID: ");
        String customerId = scanner.nextLine().trim();
        System.out.print("Enter ticket description: ");
        String description = scanner.nextLine().trim();

        if (description.isEmpty()) {
            System.out.println("❌ Description cannot be empty.");
            return;
        }

        Ticket ticket = service.createTicket(customerId, description);
        System.out.println("✓ Ticket created successfully!");
        System.out.println("  Ticket ID: " + ticket.getId());
    }

    private static void listCustomers(CustomerService service) {
        System.out.println("\n=== All Customers ===");
        var customers = service.listAll();
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
        } else {
            customers.forEach(System.out::println);
        }
    }

    private static void viewCustomerDetails(Scanner scanner, CustomerService customerService,
                                            UsageService usageService, TicketService ticketService,
                                            HotspotService hotspotService, BillingService billingService) {
        System.out.print("Enter customer ID: ");
        String customerId = scanner.nextLine().trim();

        var customerOpt = customerService.findById(customerId);
        if (customerOpt.isEmpty()) {
            System.out.println("❌ Customer not found.");
            return;
        }

        Customer customer = customerOpt.get();
        System.out.println("\n=== Customer Details ===");
        System.out.println(customer);

        System.out.println("\n--- Network Usage ---");
        var usage = usageService.getUsageForCustomer(customerId);
        if (usage.isEmpty()) {
            System.out.println("No usage records.");
        } else {
            usage.forEach(System.out::println);
        }

        System.out.println("\n--- Hotspot Device History ---");
        var devices = hotspotService.getDeviceHistory(customerId);
        if (devices.isEmpty()) {
            System.out.println("No device connections.");
        } else {
            devices.forEach(System.out::println);
            System.out.println(hotspotService.getConnectionStats(customerId));
        }

        System.out.println("\n--- Billing Summary ---");
        System.out.println(billingService.getBillingSummary(customerId));

        System.out.println("\n--- Support Tickets ---");
        var tickets = ticketService.findByCustomer(customerId);
        if (tickets.isEmpty()) {
            System.out.println("No tickets.");
        } else {
            tickets.forEach(System.out::println);
        }
    }

    private static void listTickets(TicketService service) {
        System.out.println("\n=== All Support Tickets ===");
        var tickets = service.listAllTickets();
        if (tickets.isEmpty()) {
            System.out.println("No tickets found.");
        } else {
            tickets.forEach(System.out::println);
        }
    }

    private static void updateTicketStatus(Scanner scanner, TicketService service) {
        System.out.print("Enter ticket ID: ");
        String ticketId = scanner.nextLine().trim();
        System.out.println("Select status:");
        System.out.println("1. OPEN  2. IN_PROGRESS  3. RESOLVED  4. CLOSED");
        System.out.print("Enter choice: ");
        String statusChoice = scanner.nextLine().trim();

        Ticket.Status status;
        switch (statusChoice) {
            case "1": status = Ticket.Status.OPEN; break;
            case "2": status = Ticket.Status.IN_PROGRESS; break;
            case "3": status = Ticket.Status.RESOLVED; break;
            case "4": status = Ticket.Status.CLOSED; break;
            default:
                System.out.println("❌ Invalid status choice.");
                return;
        }

        service.updateTicketStatus(ticketId, status);
        System.out.println("✓ Ticket status updated!");
    }

    private static void hotspotMenu(Scanner scanner, HotspotService service) {
        boolean inHotspotMenu = true;
        while (inHotspotMenu) {
            displayHotspotMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        connectDevice(scanner, service);
                        break;
                    case "2":
                        disconnectDevice(scanner, service);
                        break;
                    case "3":
                        updateDeviceUsage(scanner, service);
                        break;
                    case "4":
                        viewDeviceHistory(scanner, service);
                        break;
                    case "5":
                        viewActiveDevices(scanner, service);
                        break;
                    case "6":
                        viewAllActiveDevices(service);
                        break;
                    case "0":
                        inHotspotMenu = false;
                        break;
                    default:
                        System.out.println("❌ Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }

    private static void displayHotspotMenu() {
        System.out.println("\n┌─────────────────────────────────────┐");
        System.out.println("│       HOTSPOT MANAGEMENT            │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 1. Connect New Device               │");
        System.out.println("│ 2. Disconnect Device                │");
        System.out.println("│ 3. Update Device Data Usage         │");
        System.out.println("│ 4. View Device History (Customer)   │");
        System.out.println("│ 5. View Active Devices (Customer)   │");
        System.out.println("│ 6. View All Active Devices          │");
        System.out.println("│ 0. Back to Main Menu                │");
        System.out.println("└─────────────────────────────────────┘");
        System.out.print("Enter your choice: ");
    }

    private static void connectDevice(Scanner scanner, HotspotService service) {
        System.out.print("Enter customer ID: ");
        String customerId = scanner.nextLine().trim();
        System.out.print("Enter device name: ");
        String deviceName = scanner.nextLine().trim();
        System.out.print("Enter MAC address: ");
        String macAddress = scanner.nextLine().trim();

        if (deviceName.isEmpty() || macAddress.isEmpty()) {
            System.out.println("❌ Device name and MAC address cannot be empty.");
            return;
        }

        DeviceConnection device = service.connectDevice(customerId, deviceName, macAddress);
        System.out.println("✓ Device connected successfully!");
        System.out.println("  Connection ID: " + device.getId());
    }

    private static void disconnectDevice(Scanner scanner, HotspotService service) {
        System.out.print("Enter connection ID: ");
        String connectionId = scanner.nextLine().trim();
        System.out.print("Enter final data used (GB): ");
        String dataStr = scanner.nextLine().trim();

        try {
            double data = Double.parseDouble(dataStr);
            if (data < 0) {
                System.out.println("❌ Data usage cannot be negative.");
                return;
            }
            service.disconnectDevice(connectionId, data);
            System.out.println("✓ Device disconnected successfully!");
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format.");
        }
    }

    private static void updateDeviceUsage(Scanner scanner, HotspotService service) {
        System.out.print("Enter connection ID: ");
        String connectionId = scanner.nextLine().trim();
        System.out.print("Enter additional data used (GB): ");
        String dataStr = scanner.nextLine().trim();

        try {
            double data = Double.parseDouble(dataStr);
            if (data <= 0) {
                System.out.println("❌ Additional data must be positive.");
                return;
            }
            service.updateDeviceUsage(connectionId, data);
            System.out.println("✓ Device usage updated!");
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format.");
        }
    }

    private static void viewDeviceHistory(Scanner scanner, HotspotService service) {
        System.out.print("Enter customer ID: ");
        String customerId = scanner.nextLine().trim();

        System.out.println("\n=== Device Connection History ===");
        var devices = service.getDeviceHistory(customerId);
        if (devices.isEmpty()) {
            System.out.println("No device connections found.");
        } else {
            devices.forEach(System.out::println);
            System.out.println("\n" + service.getConnectionStats(customerId));
        }
    }

    private static void viewActiveDevices(Scanner scanner, HotspotService service) {
        System.out.print("Enter customer ID: ");
        String customerId = scanner.nextLine().trim();

        System.out.println("\n=== Active Device Connections ===");
        var devices = service.getActiveDevices(customerId);
        if (devices.isEmpty()) {
            System.out.println("No active device connections.");
        } else {
            devices.forEach(System.out::println);
        }
    }

    private static void viewAllActiveDevices(HotspotService service) {
        System.out.println("\n=== All Active Device Connections ===");
        var devices = service.getAllActiveDevices();
        if (devices.isEmpty()) {
            System.out.println("No active device connections.");
        } else {
            devices.forEach(System.out::println);
        }
    }
}
