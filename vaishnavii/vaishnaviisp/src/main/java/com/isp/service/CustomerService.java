package com.isp.service;

import com.isp.model.Customer;
import com.isp.repo.CustomerRepository;
import com.isp.util.IdGenerator;

import java.util.Collection;
import java.util.Optional;

/**
 * Service for managing customer accounts.
 */
public class CustomerService {
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Customer createCustomer(String name, String email) {
        String id = IdGenerator.generate();
        Customer customer = new Customer(id, name, email);
        repository.save(customer);
        System.out.println("Customer created: " + customer);
        return customer;
    }

    public Optional<Customer> findById(String id) {
        return repository.findById(id);
    }

    public Collection<Customer> listAll() {
        return repository.findAll();
    }

    public void updateCustomer(Customer customer) {
        repository.save(customer);
        System.out.println("Customer updated: " + customer);
    }

    public void deleteCustomer(String id) {
        repository.delete(id);
        System.out.println("Customer deleted: " + id);
    }
}
