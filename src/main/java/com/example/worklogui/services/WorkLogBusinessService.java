package com.example.worklogui.services;

import com.example.worklogui.RegistroTrabalho;
import com.example.worklogui.CompanyRateService;
import com.example.worklogui.RateInfo;
import com.example.worklogui.exceptions.WorkLogServiceException;
import com.example.worklogui.exceptions.WorkLogValidationException;
import com.example.worklogui.exceptions.WorkLogNotFoundException;
import com.example.worklogui.utils.DateUtils;
import com.example.worklogui.utils.ValidationHelper;
import com.example.worklogui.utils.ErrorHandler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for work log business operations.
 * Handles all business logic related to work log calculations and operations.
 */
public class WorkLogBusinessService {
    
    private final WorkLogFileManager fileManager;
    
    public WorkLogBusinessService(WorkLogFileManager fileManager) {
        this.fileManager = fileManager;
    }
    
    /**
     * Creates a new work log entry with validation and business rules
     */
    public RegistroTrabalho createWorkLog(LocalDate date, String company, double timeValue, boolean doublePay) 
            throws WorkLogValidationException, WorkLogServiceException {
        
        // Validate inputs
        String dateString = DateUtils.formatDisplayDate(date);
        ValidationHelper.WorkLogValidationResult validation =
                ValidationHelper.validateWorkLogEntry(dateString, company, String.valueOf(timeValue));

        if (!validation.isValid()) {
            throw new WorkLogValidationException(validation.getErrorMessage());
        }

        // Get rate info for company
        RateInfo info = CompanyRateService.getInstance().getRateInfoMap()
                .getOrDefault(company, new RateInfo(0.0, "hour"));

        // Create new work log entry
        RegistroTrabalho newEntry = new RegistroTrabalho();
        newEntry.setData(dateString);
        newEntry.setEmpresa(company);
        newEntry.setPagamentoDobrado(doublePay);
        newEntry.setTaxaUsada(info.getValor());
        newEntry.setTipoUsado(info.getTipo());

        if (info.getTipo().equalsIgnoreCase("minuto")) {
            newEntry.setHoras(0);
            newEntry.setMinutos(timeValue);
        } else {
            newEntry.setHoras(timeValue);
            newEntry.setMinutos(0);
        }

        try {
            fileManager.addWorkLog(newEntry);
            return newEntry;
        } catch (Exception e) {
            throw new WorkLogServiceException("Failed to save work log: " + e.getMessage(), e);
        }
    }
    
    /**
     * Updates an existing work log entry
     */
    public void updateWorkLog(RegistroTrabalho oldLog, RegistroTrabalho newLog) 
            throws WorkLogNotFoundException, WorkLogServiceException {
        try {
            boolean updated = fileManager.updateWorkLog(oldLog, newLog);
            if (!updated) {
                throw new WorkLogNotFoundException("Work log entry not found for update");
            }
        } catch (WorkLogNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkLogServiceException("Failed to update work log: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deletes a work log entry
     */
    public void deleteWorkLog(RegistroTrabalho registro) 
            throws WorkLogNotFoundException, WorkLogServiceException {
        try {
            boolean removed = fileManager.removeWorkLog(registro);
            if (!removed) {
                throw new WorkLogNotFoundException("Work log entry not found for deletion");
            }
        } catch (WorkLogNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkLogServiceException("Failed to delete work log: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculates earnings for a work log entry
     */
    public double calculateEarnings(RegistroTrabalho registro) {
        double taxa = registro.getTaxaUsada();
        String tipo = registro.getTipoUsado();
        if (tipo == null) tipo = "hour";

        double ganho = tipo.equalsIgnoreCase("minuto") ? 
            registro.getMinutos() * taxa : 
            registro.getHoras() * taxa;
            
        return registro.isPagamentoDobrado() ? ganho * 2 : ganho;
    }
    
    /**
     * Calculates total time summary by company
     */
    public String calculateTimeTotal() {
        Map<String, double[]> totais = new HashMap<>();

        try {
            List<RegistroTrabalho> allLogs = fileManager.getAllWorkLogs();
            for (RegistroTrabalho r : allLogs) {
                String empresa = r.getEmpresa();
                totais.putIfAbsent(empresa, new double[2]);
                totais.get(empresa)[0] += r.getHoras();
                totais.get(empresa)[1] += r.getMinutos();
            }
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("calculating time total", e);
            return "Error calculating time total: " + e.getMessage();
        }

        StringBuilder sb = new StringBuilder("Total Summary / Resumo Total:\n\n");

        for (Map.Entry<String, double[]> e : totais.entrySet()) {
            String empresa = e.getKey();
            double horas = e.getValue()[0];
            double minutos = e.getValue()[1];

            if (minutos >= 60) {
                int horasFromMinutos = (int) (minutos / 60);
                horas += horasFromMinutos;
                minutos = minutos % 60;
            }

            double totalHoras = horas + (minutos / 60.0);
            sb.append(String.format("%s: %.2f hours, %.2f minutes\n", empresa, horas, minutos));
            sb.append(String.format(" → Total in hours: %.2f\n\n", totalHoras));
        }

        return sb.toString();
    }
    
    /**
     * Calculates total earnings by company
     */
    public String calculateEarnings() {
        double total = 0;
        Map<String, Double> ganhos = new HashMap<>();

        try {
            List<RegistroTrabalho> allLogs = fileManager.getAllWorkLogs();
            for (RegistroTrabalho r : allLogs) {
                String empresa = r.getEmpresa();
                double ganho = calculateEarnings(r);
                ganhos.put(empresa, ganhos.getOrDefault(empresa, 0.0) + ganho);
                total += ganho;
            }
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("calculating earnings", e);
            return "Error calculating earnings: " + e.getMessage();
        }

        StringBuilder sb = new StringBuilder("Earnings / Ganho Total:\n\n");
        for (Map.Entry<String, Double> e : ganhos.entrySet()) {
            sb.append(String.format("%s: $%.2f\n", e.getKey(), e.getValue()));
        }
        sb.append(String.format("\nGrand Total / Total Geral: $%.2f", total));

        return sb.toString();
    }
    
    /**
     * Gets summary by month and year
     */
    public String getSummaryByMonthAndYear() {
        Map<String, Double> monthTotals = new java.util.TreeMap<>();
        Map<String, Double> yearTotals = new java.util.TreeMap<>();

        try {
            List<RegistroTrabalho> allLogs = fileManager.getAllWorkLogs();
            for (RegistroTrabalho r : allLogs) {
                try {
                    LocalDate date = DateUtils.parseDisplayDate(r.getData());
                    String year = String.valueOf(date.getYear());
                    String monthKey = DateUtils.getYearMonthKey(date);
                    double earnings = calculateEarnings(r);

                    monthTotals.put(monthKey, monthTotals.getOrDefault(monthKey, 0.0) + earnings);
                    yearTotals.put(year, yearTotals.getOrDefault(year, 0.0) + earnings);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("calculating summary", e);
            return "Error calculating summary: " + e.getMessage();
        }

        StringBuilder sb = new StringBuilder("📆 Year-Month Summary:\n\n");
        String currentYear = "";
        for (String monthKey : monthTotals.keySet()) {
            String year = DateUtils.getYearFromKey(monthKey);
            if (year != null && !year.equals(currentYear)) {
                if (!currentYear.isEmpty()) {
                    sb.append(String.format("%s Grand Total → $%.2f\n\n", currentYear, yearTotals.get(currentYear)));
                }
                currentYear = year;
                sb.append("📅 ").append(year).append("\n-------------------\n");
            }
            sb.append(String.format("%s → $%.2f\n-----------------------\n", monthKey, monthTotals.get(monthKey)));
        }

        if (!currentYear.isEmpty()) {
            sb.append(String.format("%s Grand Total → $%.2f\n", currentYear, yearTotals.get(currentYear)));
        }

        return sb.toString();
    }
    
    /**
     * Gets all work logs
     */
    public List<RegistroTrabalho> getAllWorkLogs() {
        try {
            return fileManager.getAllWorkLogs();
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("loading all work logs", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets filtered work logs
     */
    public List<RegistroTrabalho> getFilteredWorkLogs(String year, String month, String company) {
        try {
            return fileManager.getFilteredWorkLogs(year, month, company);
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("filtering work logs", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets work logs for a specific month
     */
    public List<RegistroTrabalho> getWorkLogsForMonth(String yearMonthKey) {
        try {
            return fileManager.getWorkLogs(yearMonthKey);
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("loading work logs for " + yearMonthKey, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Clears work log cache
     */
    public void clearCache() {
        fileManager.clearCache();
    }
    
    /**
     * Clears cache for specific month
     */
    public void clearCache(String yearMonthKey) {
        fileManager.clearCache(yearMonthKey);
    }
}