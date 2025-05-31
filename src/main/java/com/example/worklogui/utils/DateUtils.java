package com.example.worklogui.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Utility class for date operations and formatting
 */
public class DateUtils {

    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
    public static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Parse date string in MM/dd/yyyy format
     */
    public static LocalDate parseDisplayDate(String dateString) throws DateTimeParseException {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new DateTimeParseException("Date string is empty", dateString, 0);
        }
        return LocalDate.parse(dateString.trim(), DISPLAY_FORMATTER);
    }

    /**
     * Format date to MM/dd/yyyy format
     */
    public static String formatDisplayDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_FORMATTER) : "";
    }

    /**
     * Get year-month key from date (YYYY-MM format)
     */
    public static String getYearMonthKey(LocalDate date) {
        return date != null ? date.format(YEAR_MONTH_FORMATTER) : null;
    }

    /**
     * Get year-month key from date string
     */
    public static String getYearMonthKeyFromDateString(String dateString) {
        try {
            LocalDate date = parseDisplayDate(dateString);
            return getYearMonthKey(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parse year-month key to get year and month
     */
    public static LocalDate parseYearMonthKey(String yearMonthKey) {
        if (yearMonthKey == null || !yearMonthKey.matches("\\d{4}-\\d{2}")) {
            return null;
        }
        try {
            return LocalDate.parse(yearMonthKey + "-01");
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Get current year-month key
     */
    public static String getCurrentYearMonthKey() {
        return getYearMonthKey(LocalDate.now());
    }

    /**
     * Check if date string is valid
     */
    public static boolean isValidDateString(String dateString) {
        try {
            parseDisplayDate(dateString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Get year from year-month key
     */
    public static String getYearFromKey(String yearMonthKey) {
        return yearMonthKey != null && yearMonthKey.length() >= 4 ? yearMonthKey.substring(0, 4) : null;
    }

    /**
     * Get month from year-month key
     */
    public static String getMonthFromKey(String yearMonthKey) {
        return yearMonthKey != null && yearMonthKey.length() >= 7 ? yearMonthKey.substring(5, 7) : null;
    }

    /**
     * Create year-month key from year and month strings
     */
    public static String createYearMonthKey(String year, String month) {
        if (year == null || month == null) return null;
        return year + "-" + String.format("%02d", Integer.parseInt(month));
    }
}