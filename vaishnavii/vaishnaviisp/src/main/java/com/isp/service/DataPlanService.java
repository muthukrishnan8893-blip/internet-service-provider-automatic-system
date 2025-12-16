package com.isp.service;

import com.isp.model.DataPlan;
import com.isp.repo.DataPlanRepository;
import com.isp.util.IdGenerator;

import java.util.Collection;
import java.util.Optional;

/**
 * Service for managing data plans.
 */
public class DataPlanService {
    private final DataPlanRepository repository;

    public DataPlanService(DataPlanRepository repository) {
        this.repository = repository;
    }

    /**
     * Initialize default data plans.
     */
    public void initializeDefaultPlans() {
        repository.save(new DataPlan(IdGenerator.generate(), "Basic", 50, 199, "50GB/month - Basic plan"));
        repository.save(new DataPlan(IdGenerator.generate(), "Standard", 100, 299, "100GB/month - Standard plan"));
        repository.save(new DataPlan(IdGenerator.generate(), "Premium", 200, 399, "200GB/month - Premium plan"));
        repository.save(new DataPlan(IdGenerator.generate(), "Unlimited", 500, 499, "500GB/month - Unlimited plan"));
        System.out.println("Default data plans initialized");
    }

    public Optional<DataPlan> findById(String id) {
        return repository.findById(id);
    }

    public Collection<DataPlan> listAll() {
        return repository.findAll();
    }

    public void addPlan(DataPlan plan) {
        repository.save(plan);
        System.out.println("Data plan added: " + plan);
    }

    public void deletePlan(String id) {
        repository.delete(id);
    }
}
