package com.example.worklogui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AGICalculator {

    private static final double NESE_FACTOR = 0.9235; // SSA factor for NESE calculation
    private static final double SE_TAX_RATE = 0.153; // 15.3% self-employment tax
    private static final double SE_TAX_DEDUCTION_FACTOR = 0.5; // 50% of SE tax is deductible
    private static final double HOME_OFFICE_PERCENTAGE = 0.09; // 9% of home used for business

    public static class AGIResult {
        public final double grossIncome;
        public final double businessExpenses;
        public final double netEarnings;
        public final double nese; // Net Earnings from Self-Employment
        public final double selfEmploymentTax;
        public final double selfEmploymentTaxDeduction;
        public final double adjustedGrossIncome;
        public final double monthlySSACountableIncome;

        // Category breakdowns
        public final Map<ExpenseCategory, Double> expensesByCategory;

        public AGIResult(double grossIncome, double businessExpenses, double netEarnings,
                         double nese, double selfEmploymentTax, double selfEmploymentTaxDeduction,
                         double adjustedGrossIncome, Map<ExpenseCategory, Double> expensesByCategory,
                         boolean isMonthlyData) {
            this.grossIncome = grossIncome;
            this.businessExpenses = businessExpenses;
            this.netEarnings = netEarnings;
            this.nese = nese;
            this.selfEmploymentTax = selfEmploymentTax;
            this.selfEmploymentTaxDeduction = selfEmploymentTaxDeduction;
            this.adjustedGrossIncome = adjustedGrossIncome;

            // If data is already monthly, don't divide by 12
            this.monthlySSACountableIncome = isMonthlyData ? nese : nese / 12.0;

            this.expensesByCategory = expensesByCategory;
        }
    }

    public static AGIResult calculateAGI(List<RegistroTrabalho> registros, List<Bill> bills) {
        // By default, assume data is monthly
        return calculateAGI(registros, bills, true);
    }

    public static AGIResult calculateAGI(List<RegistroTrabalho> registros, List<Bill> bills, boolean isMonthlyData) {
        // 1. Calculate gross income from work
        double grossIncome = registros.stream()
                .mapToDouble(r -> {
                    double taxa = r.getTaxaUsada();
                    String tipo = r.getTipoUsado();
                    if (tipo == null) tipo = "hour";

                    double ganho = tipo.equalsIgnoreCase("minuto")
                            ? r.getMinutos() * taxa
                            : r.getHoras() * taxa;
                    return r.isPagamentoDobrado() ? ganho * 2 : ganho;
                })
                .sum();

        // 2. Calculate business expenses by category
        Map<ExpenseCategory, Double> expensesByCategory = new HashMap<>();
        double totalBusinessExpenses = 0;

        for (Bill bill : bills) {
            if (bill.getCategory() != null && bill.getCategory().isDeductible()) {
                ExpenseCategory category = bill.getCategory();
                double billAmount = bill.getAmount();
                double deductibleAmount = billAmount;

                // Apply home office percentage to appropriate categories
                if (isHomeOfficeExpense(category)) {
                    deductibleAmount = billAmount * HOME_OFFICE_PERCENTAGE;
                }

                expensesByCategory.merge(category, deductibleAmount, Double::sum);
                totalBusinessExpenses += deductibleAmount;
            }
        }

        // 3. Calculate net earnings (gross - business expenses)
        double netEarnings = grossIncome - totalBusinessExpenses;

        // 4. Calculate NESE (Net Earnings from Self-Employment)
        // NESE = Net Earnings × 0.9235
        double nese = Math.max(0, netEarnings * NESE_FACTOR);

        // 5. Calculate self-employment tax
        // SE Tax = NESE × 15.3%
        double selfEmploymentTax = nese * SE_TAX_RATE;

        // 6. Calculate SE tax deduction
        // Deduction = SE Tax × 50%
        double selfEmploymentTaxDeduction = selfEmploymentTax * SE_TAX_DEDUCTION_FACTOR;

        // 7. Calculate AGI
        // AGI = Net Earnings - SE Tax Deduction
        double adjustedGrossIncome = Math.max(0, netEarnings - selfEmploymentTaxDeduction);

        return new AGIResult(
                grossIncome,
                totalBusinessExpenses,
                netEarnings,
                nese,
                selfEmploymentTax,
                selfEmploymentTaxDeduction,
                adjustedGrossIncome,
                expensesByCategory,
                isMonthlyData
        );
    }

    // Helper method to determine if a category is a home office expense
    public static boolean isHomeOfficeExpense(ExpenseCategory category) {
        return category == ExpenseCategory.UTILITIES ||
                category == ExpenseCategory.OFFICE_RENT ||
                category == ExpenseCategory.HOME_OFFICE ||
                category == ExpenseCategory.REPAIRS_MAINTENANCE;
    }

    // Helper method to check if monthly income exceeds SGA limit
    public static boolean exceedsSGALimit(double monthlyNESE, int year) {
        double sgaLimit = getSGALimit(year);
        return monthlyNESE > sgaLimit;
    }

    // Get SGA limit for a specific year
    public static double getSGALimit(int year) {
        switch (year) {
            case 2025:
                return 1620.0;
            case 2024:
                return 1550.0;
            case 2023:
                return 1470.0;
            default:
                return 1620.0; // Default to 2025 limit
        }
    }

    // Calculate what the user can earn without exceeding the limit
    public static double remainingAllowableEarnings(AGIResult agiResult, int currentMonth, int year) {
        double monthlyNESE = agiResult.monthlySSACountableIncome;
        double monthlyLimit = getSGALimit(year);
        double yearlyLimit = monthlyLimit * 12;
        double earnedSoFar = monthlyNESE * currentMonth;
        return Math.max(0, yearlyLimit - earnedSoFar);
    }
}