package com.example.worklogui.utils;

import com.example.worklogui.RegistroTrabalho;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helper class for filtering operations
 */
public class FilterHelper {

    /**
     * Filter work logs by year, month, and company
     */
    public static List<RegistroTrabalho> applyFilters(List<RegistroTrabalho> logs, String year, String month, String company) {
        List<RegistroTrabalho> filtered = new ArrayList<>();

        for (RegistroTrabalho log : logs) {
            if (matchesFilters(log, year, month, company)) {
                filtered.add(log);
            }
        }

        return filtered;
    }

    /**
     * Check if a single log entry matches the filters
     */
    public static boolean matchesFilters(RegistroTrabalho log, String year, String month, String company) {
        try {
            LocalDate date = DateUtils.parseDisplayDate(log.getData());

            // Simplified boolean return - combine all filter checks
            return isFilterMatch(year, String.valueOf(date.getYear())) &&
                   isFilterMatch(month, String.format("%02d", date.getMonthValue())) &&
                   isFilterMatch(company, log.getEmpresa());
        } catch (Exception e) {
            System.err.println("⚠ Skipping log with invalid date: " + log.getData());
            return false;
        }
    }

    /**
     * Helper method to check if filter matches value
     */
    private static boolean isFilterMatch(String filter, String value) {
        return filter == null || filter.isEmpty() || "All".equals(filter) || filter.equals(value);
    }

    /**
     * Extract available years from work logs
     */
    public static Set<String> extractYears(List<RegistroTrabalho> logs) {
        Set<String> years = new TreeSet<>();

        for (RegistroTrabalho log : logs) {
            try {
                LocalDate date = DateUtils.parseDisplayDate(log.getData());
                years.add(String.valueOf(date.getYear()));
            } catch (Exception e) {
                System.err.println("⚠ Skipping log with invalid date: " + log.getData());
            }
        }

        return years;
    }

    /**
     * Extract available months from work logs
     */
    public static Set<String> extractMonths(List<RegistroTrabalho> logs) {
        Set<String> months = new TreeSet<>();

        for (RegistroTrabalho log : logs) {
            try {
                LocalDate date = DateUtils.parseDisplayDate(log.getData());
                months.add(String.format("%02d", date.getMonthValue()));
            } catch (Exception e) {
                System.err.println("⚠ Skipping log with invalid date: " + log.getData());
            }
        }

        return months;
    }

    /**
     * Extract available companies from work logs
     */
    public static Set<String> extractCompanies(List<RegistroTrabalho> logs) {
        Set<String> companies = new TreeSet<>();

        for (RegistroTrabalho log : logs) {
            if (log.getEmpresa() != null && !log.getEmpresa().trim().isEmpty()) {
                companies.add(log.getEmpresa());
            }
        }

        return companies;
    }

    /**
     * Build year-to-months mapping from work logs
     */
    public static Map<String, List<String>> buildYearToMonthsMap(List<RegistroTrabalho> logs) {
        Map<String, List<String>> yearToMonthsMap = new HashMap<>();

        for (RegistroTrabalho log : logs) {
            try {
                LocalDate date = DateUtils.parseDisplayDate(log.getData());
                String year = String.valueOf(date.getYear());
                String month = String.format("%02d", date.getMonthValue());

                List<String> months = yearToMonthsMap.computeIfAbsent(year, k -> new ArrayList<>());
                if (!months.contains(month)) {
                    months.add(month);
                    Collections.sort(months);
                }
            } catch (Exception e) {
                System.err.println("⚠ Skipping log with invalid date: " + log.getData());
            }
        }

        return yearToMonthsMap;
    }

    /**
     * Get logs for specific year-month
     */
    public static List<RegistroTrabalho> getLogsForYearMonth(List<RegistroTrabalho> logs, String yearMonthKey) {
        List<RegistroTrabalho> filtered = new ArrayList<>();

        for (RegistroTrabalho log : logs) {
            String logYearMonth = DateUtils.getYearMonthKeyFromDateString(log.getData());
            if (yearMonthKey.equals(logYearMonth)) {
                filtered.add(log);
            }
        }

        return filtered;
    }

    /**
     * Group logs by year-month
     */
    public static Map<String, List<RegistroTrabalho>> groupByYearMonth(List<RegistroTrabalho> logs) {
        Map<String, List<RegistroTrabalho>> grouped = new HashMap<>();

        for (RegistroTrabalho log : logs) {
            String yearMonthKey = DateUtils.getYearMonthKeyFromDateString(log.getData());
            if (yearMonthKey != null) {
                grouped.computeIfAbsent(yearMonthKey, k -> new ArrayList<>()).add(log);
            }
        }

        return grouped;
    }

    /**
     * Check if filters represent a specific month (not "All")
     */
    public static boolean isSpecificMonthFilter(String year, String month) {
        return year != null && !year.equals("All") &&
                month != null && !month.equals("All");
    }

    /**
     * Check if any filter is active (not "All")
     */
    public static boolean hasActiveFilters(String year, String month, String company) {
        return (year != null && !year.equals("All")) ||
                (month != null && !month.equals("All")) ||
                (company != null && !company.equals("All"));
    }

    /**
     * Create filter summary string for display
     */
    public static String createFilterSummary(String year, String month, String company) {
        List<String> parts = new ArrayList<>();

        if (year != null && !year.equals("All")) {
            parts.add("Year: " + year);
        }

        if (month != null && !month.equals("All")) {
            parts.add("Month: " + month);
        }

        if (company != null && !company.equals("All")) {
            parts.add("Company: " + company);
        }

        if (parts.isEmpty()) {
            return "All Records";
        }

        return String.join(", ", parts);
    }}