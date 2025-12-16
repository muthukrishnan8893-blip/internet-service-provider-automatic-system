package com.isp.repo;

import com.isp.model.Customer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory repository for customer data.
 */
public class CustomerRepository {
    private final Map<String, Customer> store = new HashMap<>();

    public void save(Customer customer) {
        store.put(customer.getId(), customer);
    }

    public Optional<Customer> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Collection<Customer> findAll() {
        return store.values();
    }

    public void delete(String id) {
        store.remove(id);
    }
}
