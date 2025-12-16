package com.isp.service;

import com.isp.model.NetworkUsage;
import com.isp.repo.UsageRepository;
import com.isp.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for tracking network usage.
 */
public class UsageService {
    private final UsageRepository repository;

    public UsageService(UsageRepository repository) {
        this.repository = repository;
    }

    public NetworkUsage recordUsage(String customerId, double gigabytes) {
        NetworkUsage usage = new NetworkUsage(
                IdGenerator.generate(),
                customerId,
                gigabytes,
                LocalDateTime.now()
        );
        repository.save(usage);
        System.out.println("Usage recorded: " + usage);
        return usage;
    }

    public List<NetworkUsage> getUsageForCustomer(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    public double getTotalUsageForCustomer(String customerId) {
        return repository.findByCustomerId(customerId).stream()
                .mapToDouble(NetworkUsage::getGigabytes)
                .sum();
    }
}
