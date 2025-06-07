package com.example.worklogui;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true) // This will ignore unknown fields
public class Bill {
    private LocalDate date;

    @JsonAlias("label")
    private String description;

    private double amount;
    private boolean paid;

    // Support both old and new format
    @JsonProperty("category")
    private ExpenseCategory category;

    @JsonProperty("deductible")
    private Boolean legacyDeductible; // For backward compatibility

    public Bill() {
        this.category = ExpenseCategory.NONE; // Default category
    }

    public Bill(LocalDate date, String description, double amount, boolean paid) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.paid = paid;
        this.category = ExpenseCategory.NONE;
    }

    public Bill(LocalDate date, String description, double amount, boolean paid, ExpenseCategory category) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.paid = paid;
        this.category = category;
    }

    // Post-construction initialization to handle legacy data
    @JsonIgnore
    public void initializeCategory() {
        if (category == null && legacyDeductible != null) {
            // Convert legacy deductible to category
            category = legacyDeductible ? ExpenseCategory.OTHER_DEDUCTIBLE : ExpenseCategory.PERSONAL;
        } else if (category == null) {
            category = ExpenseCategory.NONE;
        }
    }

    // Getters and setters
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

    @JsonProperty("category")
    public ExpenseCategory getCategory() {
        if (category == null) {
            initializeCategory();
        }
        return category;
    }

    @JsonProperty("category")
    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    // Backward compatibility getter for deductible
    @JsonIgnore
    public boolean isDeductible() {
        return getCategory() != null && getCategory().isDeductible();
    }

    // For serialization - only include category in JSON output
    @JsonIgnore
    public Boolean getLegacyDeductible() {
        return legacyDeductible;
    }

    public void setLegacyDeductible(Boolean legacyDeductible) {
        this.legacyDeductible = legacyDeductible;
        initializeCategory(); // Convert to category when set
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
                category == bill.category &&
                Objects.equals(date, bill.date) &&
                Objects.equals(description, bill.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, description, amount, paid, category);
    }
}