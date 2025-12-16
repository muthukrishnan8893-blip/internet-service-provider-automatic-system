package com.isp.service;

import com.isp.model.Customer;
import com.isp.model.NetworkUsage;
import com.isp.repo.CustomerRepository;
import com.isp.repo.UsageRepository;

import java.util.List;

/**
 * Service for automated billing calculations.
 * 
 * Billing Rules:
 * - Base monthly fee: $20.00
 * - Cost per GB: $0.50
 */
public class BillingService {
    private final CustomerRepository customerRepository;
    private final UsageRepository usageRepository;
    
    private static final double BASE_FEE = 20.0;
    private static final double COST_PER_GB = 0.5;

    public BillingService(CustomerRepository customerRepository, UsageRepository usageRepository) {
        this.customerRepository = customerRepository;
        this.usageRepository = usageRepository;
    }

    /**
     * Calculate bill for a specific customer.
     */
    public double calculateBillForCustomer(String customerId) {
        List<NetworkUsage> usage = usageRepository.findByCustomerId(customerId);
        double totalGb = usage.stream()
                .mapToDouble(NetworkUsage::getGigabytes)
                .sum();
        
        return BASE_FEE + (totalGb * COST_PER_GB);
    }

    /**
     * Run billing cycle for all customers.
     */
    public void runBillingCycle() {
        System.out.println("\n=== Running Billing Cycle ===");
        for (Customer customer : customerRepository.findAll()) {
            double amount = calculateBillForCustomer(customer.getId());
            System.out.printf("Bill for %s (%s): $%.2f%n", 
                    customer.getName(), 
                    customer.getId(), 
                    amount);
        }
        System.out.println("=== Billing Cycle Complete ===\n");
    }

    /**
     * Get billing summary for a customer.
     */
    public String getBillingSummary(String customerId) {
        List<NetworkUsage> usage = usageRepository.findByCustomerId(customerId);
        double totalGb = usage.stream()
                .mapToDouble(NetworkUsage::getGigabytes)
                .sum();
        double bill = calculateBillForCustomer(customerId);

        return String.format("Usage: %.2f GB | Base: $%.2f | Usage Cost: $%.2f | Total: $%.2f",
                totalGb, BASE_FEE, totalGb * COST_PER_GB, bill);
    }
}
