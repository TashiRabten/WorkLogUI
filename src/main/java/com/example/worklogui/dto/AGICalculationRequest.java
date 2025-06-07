package com.example.worklogui.dto;

import com.example.worklogui.ExpenseCategory;

import java.util.Map;

/**
 * Data Transfer Object for AGI calculation parameters to reduce parameter count
 */
public class AGICalculationRequest {
    private final double grossIncome;
    private final double businessExpenses;
    private final double netEarnings;
    private final double nese;
    private final double selfEmploymentTax;
    private final double selfEmploymentTaxDeduction;
    private final double adjustedGrossIncome;
    private final Map<ExpenseCategory, Double> expensesByCategory;
    private final boolean isMonthlyData;
    
    private AGICalculationRequest(Builder builder) {
        this.grossIncome = builder.grossIncome;
        this.businessExpenses = builder.businessExpenses;
        this.netEarnings = builder.netEarnings;
        this.nese = builder.nese;
        this.selfEmploymentTax = builder.selfEmploymentTax;
        this.selfEmploymentTaxDeduction = builder.selfEmploymentTaxDeduction;
        this.adjustedGrossIncome = builder.adjustedGrossIncome;
        this.expensesByCategory = builder.expensesByCategory;
        this.isMonthlyData = builder.isMonthlyData;
    }
    
    // Getters
    public double getGrossIncome() { return grossIncome; }
    public double getBusinessExpenses() { return businessExpenses; }
    public double getNetEarnings() { return netEarnings; }
    public double getNese() { return nese; }
    public double getSelfEmploymentTax() { return selfEmploymentTax; }
    public double getSelfEmploymentTaxDeduction() { return selfEmploymentTaxDeduction; }
    public double getAdjustedGrossIncome() { return adjustedGrossIncome; }
    public Map<ExpenseCategory, Double> getExpensesByCategory() { return expensesByCategory; }
    public boolean isMonthlyData() { return isMonthlyData; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private double grossIncome;
        private double businessExpenses;
        private double netEarnings;
        private double nese;
        private double selfEmploymentTax;
        private double selfEmploymentTaxDeduction;
        private double adjustedGrossIncome;
        private Map<ExpenseCategory, Double> expensesByCategory;
        private boolean isMonthlyData;
        
        public Builder grossIncome(double grossIncome) {
            this.grossIncome = grossIncome;
            return this;
        }
        
        public Builder businessExpenses(double businessExpenses) {
            this.businessExpenses = businessExpenses;
            return this;
        }
        
        public Builder netEarnings(double netEarnings) {
            this.netEarnings = netEarnings;
            return this;
        }
        
        public Builder nese(double nese) {
            this.nese = nese;
            return this;
        }
        
        public Builder selfEmploymentTax(double selfEmploymentTax) {
            this.selfEmploymentTax = selfEmploymentTax;
            return this;
        }
        
        public Builder selfEmploymentTaxDeduction(double selfEmploymentTaxDeduction) {
            this.selfEmploymentTaxDeduction = selfEmploymentTaxDeduction;
            return this;
        }
        
        public Builder adjustedGrossIncome(double adjustedGrossIncome) {
            this.adjustedGrossIncome = adjustedGrossIncome;
            return this;
        }
        
        public Builder expensesByCategory(Map<ExpenseCategory, Double> expensesByCategory) {
            this.expensesByCategory = expensesByCategory;
            return this;
        }
        
        public Builder isMonthlyData(boolean isMonthlyData) {
            this.isMonthlyData = isMonthlyData;
            return this;
        }
        
        public AGICalculationRequest build() {
            return new AGICalculationRequest(this);
        }
    }
}