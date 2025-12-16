package com.isp.repo;

import com.isp.model.DeviceConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory repository for device connection history.
 */
public class DeviceConnectionRepository {
    private final List<DeviceConnection> store = new ArrayList<>();

    public void save(DeviceConnection connection) {
        store.add(connection);
    }

    public Optional<DeviceConnection> findById(String id) {
        return store.stream()
                .filter(d -> d.getId().equals(id))
                .findFirst();
    }

    public List<DeviceConnection> findByCustomerId(String customerId) {
        return store.stream()
                .filter(d -> d.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    public List<DeviceConnection> findActiveByCustomerId(String customerId) {
        return store.stream()
                .filter(d -> d.getCustomerId().equals(customerId) && d.isActive())
                .collect(Collectors.toList());
    }

    public List<DeviceConnection> findAll() {
        return new ArrayList<>(store);
    }

    public List<DeviceConnection> findAllActive() {
        return store.stream()
                .filter(DeviceConnection::isActive)
                .collect(Collectors.toList());
    }
}
