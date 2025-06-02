package com.example.worklogui.utils;

import com.example.worklogui.RegistroTrabalho;
import com.example.worklogui.CompanyRateService;
import com.example.worklogui.RateInfo;

/**
 * Utility class for input validation
 */
public class ValidationHelper {

    /**
     * Validate work log entry inputs
     */
    public static class WorkLogValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public WorkLogValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }

        public static WorkLogValidationResult success() {
            return new WorkLogValidationResult(true, null);
        }

        public static WorkLogValidationResult error(String message) {
            return new WorkLogValidationResult(false, message);
        }
    }

    /**
     * Validate work log entry data
     */
    public static WorkLogValidationResult validateWorkLogEntry(String dateString, String company, String timeValue) {
        // Check for null or empty fields
        if (dateString == null || dateString.trim().isEmpty()) {
            return WorkLogValidationResult.error("Date is required.\nData é obrigatória.");
        }

        if (company == null || company.trim().isEmpty()) {
            return WorkLogValidationResult.error("Company is required.\nEmpresa é obrigatória.");
        }

        if (timeValue == null || timeValue.trim().isEmpty()) {
            return WorkLogValidationResult.error("Time value is required.\nValor do tempo é obrigatório.");
        }

        // Validate date format
        if (!DateUtils.isValidDateString(dateString)) {
            return WorkLogValidationResult.error("Invalid date format. Use MM/dd/yyyy.\nFormato de data inválido. Use MM/dd/aaaa.");
        }

        // Validate time value
        try {
            double value = Double.parseDouble(timeValue.trim());
            if (value < 0) {
                return WorkLogValidationResult.error("Time value must be positive.\nValor do tempo deve ser positivo.");
            }

            // Get the rate type for this company to determine validation limits
            RateInfo rateInfo = CompanyRateService.getInstance().getRateInfoMap().get(company);
            String rateType = rateInfo != null ? rateInfo.getTipo() : "hour";

            if ("minuto".equalsIgnoreCase(rateType)) {
                // For minute-based rates, allow up to 24 hours worth of minutes (1440 minutes)
                if (value > 1440) {
                    return WorkLogValidationResult.error("Minutes cannot exceed 1440 (24 hours).\nMinutos não podem exceder 1440 (24 horas).");
                }
            } else {
                // For hour-based rates, keep the 24-hour limit
                if (value > 24) {
                    return WorkLogValidationResult.error("Hours cannot exceed 24.\nHoras não podem exceder 24.");
                }
            }
        } catch (NumberFormatException e) {
            return WorkLogValidationResult.error("Invalid time value. Please enter a number.\nValor de tempo inválido. Digite um número.");
        }

        return WorkLogValidationResult.success();
    }

    /**
     * Validate bill entry data
     */
    public static WorkLogValidationResult validateBillEntry(String dateString, String description, String amountValue) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return WorkLogValidationResult.error("Date is required.\nData é obrigatória.");
        }

        if (description == null || description.trim().isEmpty()) {
            return WorkLogValidationResult.error("Description is required.\nDescrição é obrigatória.");
        }

        if (amountValue == null || amountValue.trim().isEmpty()) {
            return WorkLogValidationResult.error("Amount is required.\nValor é obrigatório.");
        }

        if (!DateUtils.isValidDateString(dateString)) {
            return WorkLogValidationResult.error("Invalid date format. Use MM/dd/yyyy.\nFormato de data inválido. Use MM/dd/aaaa.");
        }

        try {
            double amount = Double.parseDouble(amountValue.trim());
            if (amount <= 0) {
                return WorkLogValidationResult.error("Amount must be greater than 0.\nValor deve ser maior que 0.");
            }
        } catch (NumberFormatException e) {
            return WorkLogValidationResult.error("Invalid amount. Please enter a number.\nValor inválido. Digite um número.");
        }

        return WorkLogValidationResult.success();
    }

    /**
     * Validate company rate data
     */
    public static WorkLogValidationResult validateCompanyRate(String name, String rateValue, String type) {
        if (name == null || name.trim().isEmpty()) {
            return WorkLogValidationResult.error("Company name is required.\nNome da empresa é obrigatório.");
        }

        if (rateValue == null || rateValue.trim().isEmpty()) {
            return WorkLogValidationResult.error("Rate is required.\nTaxa é obrigatória.");
        }

        if (type == null || (!type.equals("hora") && !type.equals("minuto"))) {
            return WorkLogValidationResult.error("Invalid rate type.\nTipo de taxa inválido.");
        }

        try {
            double rate = Double.parseDouble(rateValue.trim());
            if (rate <= 0) {
                return WorkLogValidationResult.error("Rate must be greater than 0.\nTaxa deve ser maior que 0.");
            }
        } catch (NumberFormatException e) {
            return WorkLogValidationResult.error("Invalid rate value.\nValor de taxa inválido.");
        }

        return WorkLogValidationResult.success();
    }

    /**
     * Validate filter values
     */
    public static boolean isValidFilter(String year, String month, String company) {
        // "All" is always valid
        if ("All".equals(year) || "All".equals(month) || "All".equals(company)) {
            return true;
        }

        // Validate year
        if (year != null && !year.matches("\\d{4}")) {
            return false;
        }

        // Validate month
        if (month != null && !month.matches("\\d{2}")) {
            return false;
        }

        return true;
    }
}