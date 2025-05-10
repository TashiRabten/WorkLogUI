package com.example.worklogui;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDate;
import java.util.Objects;

public class Bill {
    private LocalDate date;

    @JsonAlias("label")
    private String description;

    private double amount;
    private boolean paid;

    public Bill() {}

    public Bill(LocalDate date, String description, double amount, boolean paid) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.paid = paid;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getLabel() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bill)) return false;
        Bill bill = (Bill) o;
        return Double.compare(bill.amount, amount) == 0 &&
                paid == bill.paid &&
                Objects.equals(date, bill.date) &&
                Objects.equals(description, bill.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, description, amount,paid);}
}