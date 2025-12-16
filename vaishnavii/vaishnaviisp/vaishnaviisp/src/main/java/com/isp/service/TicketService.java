package com.isp.service;

import com.isp.model.Ticket;
import com.isp.repo.TicketRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing support tickets.
 */
public class TicketService {
    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    public Ticket createTicket(String customerId, String description) {
        Ticket ticket = new Ticket(customerId, description);
        repository.save(ticket);
        System.out.println("Ticket created: " + ticket);
        return ticket;
    }

    public Optional<Ticket> findById(String id) {
        return repository.findById(id);
    }

    public List<Ticket> findByCustomer(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    public void updateTicketStatus(String ticketId, Ticket.Status status) {
        Optional<Ticket> ticketOpt = repository.findById(ticketId);
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            ticket.setStatus(status);
            System.out.println("Ticket status updated: " + ticket);
        } else {
            System.out.println("Ticket not found: " + ticketId);
        }
    }

    public List<Ticket> listAllTickets() {
        return repository.findAll();
    }
}
