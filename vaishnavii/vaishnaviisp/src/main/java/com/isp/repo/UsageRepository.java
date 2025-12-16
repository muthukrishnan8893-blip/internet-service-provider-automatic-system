package com.isp.repo;

import com.isp.model.NetworkUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory repository for network usage data.
 */
public class UsageRepository {
    private final List<NetworkUsage> store = new ArrayList<>();

    public void save(NetworkUsage usage) {
        store.add(usage);
    }

    public List<NetworkUsage> findByCustomerId(String customerId) {
        return store.stream()
                .filter(u -> u.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    public List<NetworkUsage> findAll() {
        return new ArrayList<>(store);
    }
}
