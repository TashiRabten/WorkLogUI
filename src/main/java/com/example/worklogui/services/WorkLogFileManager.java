package com.example.worklogui.services;

import com.example.worklogui.RegistroTrabalho;
import com.example.worklogui.utils.DateUtils;
import com.example.worklogui.utils.ErrorHandler;
import com.example.worklogui.utils.FileOperationHelper;
import com.example.worklogui.utils.FilterHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages work log files with caching and optimized operations
 */
public class WorkLogFileManager {

    // Cache for loaded work logs by year-month key
    private final Map<String, List<RegistroTrabalho>> cache = new ConcurrentHashMap<>();

    // Track which files have been loaded to avoid unnecessary disk reads
    private final Map<String, Long> lastModified = new ConcurrentHashMap<>();

    private static final int MAX_CACHE_SIZE = 50; // Prevent memory issues

    /**
     * Initialize the file manager and required directories
     */
    public void initialize() throws ErrorHandler.FileOperationException {
        FileOperationHelper.initializeDirectories();
        clearCache();
    }

    /**
     * Get work logs for a specific year-month
     */
    public List<RegistroTrabalho> getWorkLogs(String yearMonthKey) throws ErrorHandler.FileOperationException {
        if (yearMonthKey == null) {
            return new ArrayList<>();
        }

        // Check cache first - combined nested conditions
        if (cache.containsKey(yearMonthKey) && !hasFileChanged(yearMonthKey)) {
            System.out.println("🔍 Cache hit for " + yearMonthKey);
            return new ArrayList<>(cache.get(yearMonthKey)); // Return defensive copy
        }

        // Load from disk
        System.out.println("💾 Loading from disk: " + yearMonthKey);
        List<RegistroTrabalho> logs = FileOperationHelper.loadWorkLogs(yearMonthKey);

        // Update cache
        updateCache(yearMonthKey, logs);

        return new ArrayList<>(logs); // Return defensive copy
    }

    /**
     * Save work logs for a specific year-month
     */
    public void saveWorkLogs(String yearMonthKey, List<RegistroTrabalho> logs) throws ErrorHandler.FileOperationException {
        if (yearMonthKey == null) {
            throw new ErrorHandler.ValidationException("Year-month key cannot be null");
        }

        // Save to disk
        FileOperationHelper.saveWorkLogs(yearMonthKey, logs);

        // Update cache
        updateCache(yearMonthKey, logs);

        System.out.println("✅ Saved and cached " + logs.size() + " logs for " + yearMonthKey);
    }

    /**
     * Add a single work log entry
     */
    public void addWorkLog(RegistroTrabalho log) throws ErrorHandler.FileOperationException {
        String yearMonthKey = DateUtils.getYearMonthKeyFromDateString(log.getData());
        if (yearMonthKey == null) {
            throw new ErrorHandler.ValidationException("Invalid date in work log: " + log.getData());
        }

        // Get existing logs for this month
        List<RegistroTrabalho> monthLogs = getWorkLogs(yearMonthKey);

        // Add new log
        monthLogs.add(log);

        // Save updated list
        saveWorkLogs(yearMonthKey, monthLogs);
    }

    /**
     * Remove a work log entry
     */
    public boolean removeWorkLog(RegistroTrabalho logToRemove) throws ErrorHandler.FileOperationException {
        String yearMonthKey = DateUtils.getYearMonthKeyFromDateString(logToRemove.getData());
        if (yearMonthKey == null) {
            return false;
        }

        // Get existing logs for this month
        List<RegistroTrabalho> monthLogs = getWorkLogs(yearMonthKey);

        // Remove the log (comparing by content since objects might be different)
        boolean removed = monthLogs.removeIf(log ->
                logsAreEqual(log, logToRemove)
        );

        if (removed) {
            // Save updated list
            saveWorkLogs(yearMonthKey, monthLogs);
        }

        return removed;
    }

    /**
     * Update an existing work log entry
     */
    public boolean updateWorkLog(RegistroTrabalho oldLog, RegistroTrabalho newLog) throws ErrorHandler.FileOperationException {
        String oldYearMonth = DateUtils.getYearMonthKeyFromDateString(oldLog.getData());
        String newYearMonth = DateUtils.getYearMonthKeyFromDateString(newLog.getData());

        if (oldYearMonth == null || newYearMonth == null) {
            throw new ErrorHandler.ValidationException("Invalid dates in work log update");
        }

        // If the date changed, we need to move between files
        if (!oldYearMonth.equals(newYearMonth)) {
            // Remove from old file
            boolean removed = removeWorkLog(oldLog);
            if (removed) {
                // Add to new file
                addWorkLog(newLog);
                return true;
            }
            return false;
        } else {
            // Update within same file
            List<RegistroTrabalho> monthLogs = getWorkLogs(oldYearMonth);

            // Find and replace the log
            for (int i = 0; i < monthLogs.size(); i++) {
                if (logsAreEqual(monthLogs.get(i), oldLog)) {
                    monthLogs.set(i, newLog);
                    saveWorkLogs(oldYearMonth, monthLogs);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get all work logs from all files
     */
    public List<RegistroTrabalho> getAllWorkLogs() throws ErrorHandler.FileOperationException {
        List<String> availableKeys = FileOperationHelper.getAvailableYearMonthKeys();
        List<RegistroTrabalho> allLogs = new ArrayList<>();

        for (String key : availableKeys) {
            List<RegistroTrabalho> monthLogs = getWorkLogs(key);
            allLogs.addAll(monthLogs);
        }

        return allLogs;
    }

    /**
     * Get work logs with filters applied
     */
    public List<RegistroTrabalho> getFilteredWorkLogs(String year, String month, String company) throws ErrorHandler.FileOperationException {
        // If we have specific year and month, load only that file
        if (FilterHelper.isSpecificMonthFilter(year, month)) {
            String yearMonthKey = DateUtils.createYearMonthKey(year, month);
            List<RegistroTrabalho> monthLogs = getWorkLogs(yearMonthKey);
            return FilterHelper.applyFilters(monthLogs, year, month, company);
        }

        // Otherwise, load all and filter
        List<RegistroTrabalho> allLogs = getAllWorkLogs();
        return FilterHelper.applyFilters(allLogs, year, month, company);
    }

    /**
     * Get available year-month keys
     */
    public List<String> getAvailableYearMonthKeys() throws ErrorHandler.FileOperationException {
        return FileOperationHelper.getAvailableYearMonthKeys();
    }

    /**
     * Clear all cached data
     */
    public void clearCache() {
        cache.clear();
        lastModified.clear();
        System.out.println("🔄 Work log cache cleared");
    }

    /**
     * Clear cache for specific year-month
     */
    public void clearCache(String yearMonthKey) {
        cache.remove(yearMonthKey);
        lastModified.remove(yearMonthKey);
        System.out.println("🔄 Cache cleared for " + yearMonthKey);
    }

    /**
     * Cleanup old backups
     */
    public void cleanupOldBackups() {
        FileOperationHelper.cleanupOldBackups(10); // Keep 10 most recent backups
    }

    /**
     * Check if file has changed since last load
     */
    private boolean hasFileChanged(String yearMonthKey) {
        if (!FileOperationHelper.logFileExists(yearMonthKey)) {
            return true; // File was deleted
        }

        try {
            long currentModified = java.nio.file.Files.getLastModifiedTime(
                    FileOperationHelper.getLogFilePath(yearMonthKey)
            ).toMillis();

            Long cachedModified = lastModified.get(yearMonthKey);
            return cachedModified == null || currentModified != cachedModified;
        } catch (Exception e) {
            return true; // Assume changed if we can't check
        }
    }

    /**
     * Update cache with new data
     */
    private void updateCache(String yearMonthKey, List<RegistroTrabalho> logs) {
        // Prevent cache from growing too large
        if (cache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries (simple LRU)
            String oldestKey = cache.keySet().iterator().next();
            cache.remove(oldestKey);
            lastModified.remove(oldestKey);
        }

        // Store defensive copy in cache
        cache.put(yearMonthKey, new ArrayList<>(logs));

        // Update last modified time
        try {
            if (FileOperationHelper.logFileExists(yearMonthKey)) {
                long modified = java.nio.file.Files.getLastModifiedTime(
                        FileOperationHelper.getLogFilePath(yearMonthKey)
                ).toMillis();
                lastModified.put(yearMonthKey, modified);
            }
        } catch (Exception e) {
            // Ignore if we can't get modified time
        }
    }

    /**
     * Compare two work logs for equality
     */
    private boolean logsAreEqual(RegistroTrabalho log1, RegistroTrabalho log2) {
        if (log1 == log2) return true;
        if (log1 == null || log2 == null) return false;

        return java.util.Objects.equals(log1.getData(), log2.getData()) &&
                java.util.Objects.equals(log1.getEmpresa(), log2.getEmpresa()) &&
                Double.compare(log1.getHoras(), log2.getHoras()) == 0 &&
                Double.compare(log1.getMinutos(), log2.getMinutos()) == 0 &&
                log1.isPagamentoDobrado() == log2.isPagamentoDobrado() &&
                Double.compare(log1.getTaxaUsada(), log2.getTaxaUsada()) == 0 &&
                java.util.Objects.equals(log1.getTipoUsado(), log2.getTipoUsado());
    }

    /**
     * Get cache statistics for debugging
     */
    public String getCacheStatistics() {
        return String.format("Cache: %d entries, %d tracked files",
                cache.size(), lastModified.size());
    }
}