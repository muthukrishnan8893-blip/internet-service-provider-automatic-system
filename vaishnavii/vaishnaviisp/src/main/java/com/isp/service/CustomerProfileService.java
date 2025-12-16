package com.isp.service;

import com.isp.model.CustomerProfile;
import com.isp.repo.CustomerProfileRepository;
import com.isp.util.IdGenerator;
import com.isp.model.DataPlan;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * Service for customer profile management.
 */
public class CustomerProfileService {
    private final CustomerProfileRepository repository;
    private final EmailService emailService;

    public CustomerProfileService(CustomerProfileRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    /**
     * Create a customer profile for a registered user.
     */
    public CustomerProfile createProfile(String userId, String fullName, String email) {
        String profileId = IdGenerator.generate();
        CustomerProfile profile = new CustomerProfile(profileId, userId, fullName);
        repository.save(profile);
        System.out.println("Customer profile created: " + profile);
        return profile;
    }

    /**
     * Select a plan for the customer.
     */
    public void selectPlan(String customerId, DataPlan plan, String email, String customerName) {
        Optional<CustomerProfile> profileOpt = repository.findById(customerId);
        if (profileOpt.isPresent()) {
            CustomerProfile profile = profileOpt.get();
            profile.setCurrentPlan(plan);
            profile.setPlanStartDate(LocalDateTime.now());
            profile.setPlanRenewalDate(LocalDateTime.now().plusMonths(1));
            repository.save(profile);

            System.out.println("Plan selected for customer: " + plan.getName());
            System.out.println("Sending plan confirmation email to: " + email);
            
            // Send confirmation email to customer
            emailService.sendPlanConfirmationEmail(email, customerName, plan.getName(), plan.getPricePerMonth());
            
            // Send notification to admin
            String adminSubject = "New Plan Selection - " + customerName;
            String adminBody = "Customer " + customerName + " (" + email + ") has selected the following plan:\n\n" +
                              "Plan: " + plan.getName() + "\n" +
                              "Price: $" + plan.getPricePerMonth() + "/month\n" +
                              "Data Allowance: " + plan.getDataGB() + " GB\n" +
                              "Start Date: " + LocalDateTime.now() + "\n\n" +
                              "Please ensure the plan is activated.";
            emailService.sendEmail("muthuvel04041971@gmail.com", adminSubject, adminBody);
            
            System.out.println("Plan confirmation emails sent successfully!");
        } else {
            System.err.println("ERROR: Customer profile not found: " + customerId);
        }
    }

    public Optional<CustomerProfile> findById(String id) {
        return repository.findById(id);
    }

    public Optional<CustomerProfile> findByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public Collection<CustomerProfile> listAll() {
        return repository.findAll();
    }

    public void updateProfile(CustomerProfile profile) {
        repository.save(profile);
    }
}
