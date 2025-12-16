package com.isp.repo;

import com.isp.model.Ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory repository for support tickets.
 */
public class TicketRepository {
    private final List<Ticket> store = new ArrayList<>();

    public void save(Ticket ticket) {
        store.add(ticket);
    }

    public Optional<Ticket> findById(String id) {
        return store.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    public List<Ticket> findByCustomerId(String customerId) {
        return store.stream()
                .filter(t -> t.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    public List<Ticket> findAll() {
        return new ArrayList<>(store);
    }
}
