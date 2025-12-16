package com.isp.model;

/**
 * Represents a data plan offering.
 */
public class DataPlan {
    private final String id;
    private String name;
    private double dataGB;
    private double pricePerMonth;
    private String description;

    public DataPlan(String id, String name, double dataGB, double pricePerMonth, String description) {
        this.id = id;
        this.name = name;
        this.dataGB = dataGB;
        this.pricePerMonth = pricePerMonth;
        this.description = description;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getDataGB() { return dataGB; }
    public double getPricePerMonth() { return pricePerMonth; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "DataPlan{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dataGB=" + dataGB +
                ", pricePerMonth=$" + pricePerMonth +
                '}';
    }
}
