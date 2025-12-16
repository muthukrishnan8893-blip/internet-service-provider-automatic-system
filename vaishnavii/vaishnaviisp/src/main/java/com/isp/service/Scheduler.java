package com.isp.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for automated billing cycles.
 */
public class Scheduler {
    private final ScheduledExecutorService executor;
    private final BillingService billingService;

    public Scheduler(BillingService billingService) {
        this.billingService = billingService;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Schedule automated billing to run at fixed intervals.
     * 
     * @param initialDelaySeconds delay before first execution
     * @param intervalSeconds interval between executions
     */
    public void scheduleBilling(long initialDelaySeconds, long intervalSeconds) {
        executor.scheduleAtFixedRate(() -> {
            try {
                billingService.runBillingCycle();
            } catch (Exception e) {
                System.err.println("Error running billing cycle: " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelaySeconds, intervalSeconds, TimeUnit.SECONDS);
        
        System.out.println("Automated billing scheduled: every " + intervalSeconds + " seconds");
    }

    /**
     * Shutdown the scheduler.
     */
    public void shutdown() {
        executor.shutdownNow();
        System.out.println("Scheduler shutdown");
    }
}
