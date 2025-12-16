package com.isp.service;

import com.isp.model.DeviceUsageLog;
import com.isp.repo.DeviceUsageLogRepository;
import com.isp.util.IdGenerator;

import java.util.List;

/**
 * Service for tracking detailed device usage logs.
 */
public class DeviceUsageLogService {
    private final DeviceUsageLogRepository repository;

    public DeviceUsageLogService(DeviceUsageLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Log device usage at specific time.
     */
    public DeviceUsageLog logUsage(String deviceConnectionId, String customerId, double dataUsedGB) {
        DeviceUsageLog log = new DeviceUsageLog(
                IdGenerator.generate(),
                deviceConnectionId,
                customerId,
                dataUsedGB
        );
        repository.save(log);
        return log;
    }

    /**
     * Get usage history for a specific device.
     */
    public List<DeviceUsageLog> getDeviceUsageHistory(String deviceConnectionId) {
        return repository.findByDeviceConnectionId(deviceConnectionId);
    }

    /**
     * Get all usage logs for a customer.
     */
    public List<DeviceUsageLog> getCustomerUsageLogs(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    /**
     * Calculate total data used by a device.
     */
    public double getTotalDataUsageForDevice(String deviceConnectionId) {
        return repository.findByDeviceConnectionId(deviceConnectionId).stream()
                .mapToDouble(DeviceUsageLog::getDataUsedGB)
                .sum();
    }
}
