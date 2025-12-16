package com.isp.service;

import com.isp.model.DeviceConnection;
import com.isp.repo.DeviceConnectionRepository;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HotspotServiceTest {
    
    @Test
    public void testConnectAndDisconnectDevice() {
        DeviceConnectionRepository repo = new DeviceConnectionRepository();
        HotspotService service = new HotspotService(repo);
        
        // Connect a device
        DeviceConnection device = service.connectDevice("cust-1", "iPhone 13", "AA:BB:CC:DD:EE:FF");
        assertNotNull(device);
        assertEquals("cust-1", device.getCustomerId());
        assertEquals("iPhone 13", device.getDeviceName());
        assertTrue(device.isActive());
        
        // Disconnect the device
        service.disconnectDevice(device.getId(), 2.5);
        assertFalse(device.isActive());
        assertEquals(2.5, device.getDataUsedGB(), 0.001);
    }
    
    @Test
    public void testUpdateDeviceUsage() {
        DeviceConnectionRepository repo = new DeviceConnectionRepository();
        HotspotService service = new HotspotService(repo);
        
        DeviceConnection device = service.connectDevice("cust-1", "Laptop", "11:22:33:44:55:66");
        
        service.updateDeviceUsage(device.getId(), 1.5);
        assertEquals(1.5, device.getDataUsedGB(), 0.001);
        
        service.updateDeviceUsage(device.getId(), 0.8);
        assertEquals(2.3, device.getDataUsedGB(), 0.001);
    }
    
    @Test
    public void testGetDeviceHistory() {
        DeviceConnectionRepository repo = new DeviceConnectionRepository();
        HotspotService service = new HotspotService(repo);
        
        service.connectDevice("cust-1", "Device 1", "AA:BB:CC:DD:EE:F1");
        service.connectDevice("cust-1", "Device 2", "AA:BB:CC:DD:EE:F2");
        service.connectDevice("cust-2", "Device 3", "AA:BB:CC:DD:EE:F3");
        
        List<DeviceConnection> history = service.getDeviceHistory("cust-1");
        assertEquals(2, history.size());
        
        List<DeviceConnection> activeDevices = service.getActiveDevices("cust-1");
        assertEquals(2, activeDevices.size());
    }
    
    @Test
    public void testGetTotalDataUsage() {
        DeviceConnectionRepository repo = new DeviceConnectionRepository();
        HotspotService service = new HotspotService(repo);
        
        DeviceConnection dev1 = service.connectDevice("cust-1", "Device 1", "AA:BB:CC:DD:EE:F1");
        DeviceConnection dev2 = service.connectDevice("cust-1", "Device 2", "AA:BB:CC:DD:EE:F2");
        
        service.disconnectDevice(dev1.getId(), 3.5);
        service.disconnectDevice(dev2.getId(), 2.0);
        
        double totalUsage = service.getTotalDataUsageForCustomer("cust-1");
        assertEquals(5.5, totalUsage, 0.001);
    }
}
