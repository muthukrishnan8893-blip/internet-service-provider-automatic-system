package com.isp.service;

import com.isp.model.DeviceConnection;
import com.isp.repo.DeviceConnectionRepository;
import com.isp.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing hotspot device connections and tracking usage history.
 */
public class HotspotService {
    private final DeviceConnectionRepository repository;

    public HotspotService(DeviceConnectionRepository repository) {
        this.repository = repository;
    }

    /**
     * Connect a new device to customer's hotspot.
     */
    public DeviceConnection connectDevice(String customerId, String deviceName, String macAddress) {
        String id = IdGenerator.generate();
        DeviceConnection connection = new DeviceConnection(id, customerId, deviceName, macAddress);
        repository.save(connection);
        System.out.println("Device connected: " + connection);
        return connection;
    }

    /**
     * Disconnect a device and record final data usage.
     */
    public void disconnectDevice(String connectionId, double finalDataUsedGB) {
        Optional<DeviceConnection> connectionOpt = repository.findById(connectionId);
        if (connectionOpt.isPresent()) {
            DeviceConnection connection = connectionOpt.get();
            if (connection.isActive()) {
                connection.disconnect(finalDataUsedGB);
                System.out.println("Device disconnected: " + connection);
            } else {
                System.out.println("Device already disconnected: " + connectionId);
            }
        } else {
            System.out.println("Device connection not found: " + connectionId);
        }
    }

    /**
     * Update data usage for an active device connection.
     */
    public void updateDeviceUsage(String connectionId, double additionalDataGB) {
        Optional<DeviceConnection> connectionOpt = repository.findById(connectionId);
        if (connectionOpt.isPresent()) {
            DeviceConnection connection = connectionOpt.get();
            if (connection.isActive()) {
                connection.addDataUsage(additionalDataGB);
                System.out.printf("Updated device %s: +%.2f GB (total: %.2f GB)%n",
                        connection.getDeviceName(), additionalDataGB, connection.getDataUsedGB());
            } else {
                System.out.println("Cannot update: Device is disconnected");
            }
        } else {
            System.out.println("Device connection not found: " + connectionId);
        }
    }

    /**
     * Get all device connections for a customer (history).
     */
    public List<DeviceConnection> getDeviceHistory(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    /**
     * Get currently active devices for a customer.
     */
    public List<DeviceConnection> getActiveDevices(String customerId) {
        return repository.findActiveByCustomerId(customerId);
    }

    /**
     * Get all active devices across all customers.
     */
    public List<DeviceConnection> getAllActiveDevices() {
        return repository.findAllActive();
    }

    /**
     * Calculate total data usage for a customer across all device connections.
     */
    public double getTotalDataUsageForCustomer(String customerId) {
        return repository.findByCustomerId(customerId).stream()
                .mapToDouble(DeviceConnection::getDataUsedGB)
                .sum();
    }

    /**
     * Get connection statistics for a customer.
     */
    public String getConnectionStats(String customerId) {
        List<DeviceConnection> connections = repository.findByCustomerId(customerId);
        long totalConnections = connections.size();
        long activeConnections = connections.stream().filter(DeviceConnection::isActive).count();
        double totalData = connections.stream().mapToDouble(DeviceConnection::getDataUsedGB).sum();
        
        return String.format("Total Connections: %d | Active: %d | Total Data: %.2f GB",
                totalConnections, activeConnections, totalData);
    }
}
