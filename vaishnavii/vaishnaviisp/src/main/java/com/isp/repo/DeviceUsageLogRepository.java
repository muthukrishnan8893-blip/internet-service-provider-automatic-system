package com.isp.repo;

import com.isp.model.DeviceUsageLog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for detailed device usage logs.
 */
public class DeviceUsageLogRepository {
    private final List<DeviceUsageLog> store = new ArrayList<>();

    public void save(DeviceUsageLog log) {
        store.add(log);
    }

    public List<DeviceUsageLog> findByDeviceConnectionId(String deviceConnectionId) {
        return store.stream()
                .filter(l -> l.getDeviceConnectionId().equals(deviceConnectionId))
                .collect(Collectors.toList());
    }

    public List<DeviceUsageLog> findByCustomerId(String customerId) {
        return store.stream()
                .filter(l -> l.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    public List<DeviceUsageLog> findAll() {
        return new ArrayList<>(store);
    }
}
