package com.isp.service;

import com.isp.model.NetworkUsage;
import com.isp.model.Customer;
import com.isp.repo.CustomerRepository;
import com.isp.repo.UsageRepository;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BillingServiceTest {
    @Test
    public void testCalculateBillForCustomer() {
        CustomerRepository cr = new CustomerRepository();
        UsageRepository ur = new UsageRepository();
        Customer c = new Customer("cust-1", "Alice", "a@example.com");
        cr.save(c);
        ur.save(new NetworkUsage("u1", c.getId(), 10.0, LocalDateTime.now()));
        ur.save(new NetworkUsage("u2", c.getId(), 5.0, LocalDateTime.now()));

        BillingService bs = new BillingService(cr, ur);
        double bill = bs.calculateBillForCustomer(c.getId());
        // base 20 + (15 GB * 0.5) = 20 + 7.5 = 27.5
        assertEquals(27.5, bill, 0.0001);
    }
}
