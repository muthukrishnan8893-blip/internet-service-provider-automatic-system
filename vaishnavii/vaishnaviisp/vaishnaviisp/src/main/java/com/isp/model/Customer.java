package com.isp.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a customer account in the ISP system.
 */
public class Customer {
    private final String id;
    private String name;
    private String email;
    private String planType;
    private LocalDate createdAt;

    public Customer(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.planType = "Basic";
        this.createdAt = LocalDate.now();
    }

    public String getId() { 
        return id; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public String getPlanType() { 
        return planType; 
    }
    
    public void setPlanType(String planType) { 
        this.planType = planType; 
    }
    
    public LocalDate getCreatedAt() { 
        return createdAt; 
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", plan='" + planType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
