package com.example.worklogui;

import java.time.LocalDate;

public class Bill {
    private String label;
    private double amount;
    private LocalDate date;

    public Bill() {}

    public Bill(String label, double amount, LocalDate date) {
        this.label = label;
        this.amount = amount;
        this.date = date;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
