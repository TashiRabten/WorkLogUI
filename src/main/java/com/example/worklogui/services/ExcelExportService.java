package com.example.worklogui.services;

import com.example.worklogui.*;
import com.example.worklogui.utils.ErrorHandler;
import javafx.concurrent.Task;
import javafx.application.Platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for handling Excel export operations in the background.
 * Ensures UI thread safety and provides progress callbacks.
 */
public class ExcelExportService {
    
    private final CompanyManagerService companyService;
    
    public ExcelExportService(CompanyManagerService companyService) {
        this.companyService = companyService;
    }
    
    /**
     * Exports all data to Excel asynchronously
     */
    public CompletableFuture<Void> exportAllToExcelAsync(
            Consumer<String> progressCallback,
            Consumer<Exception> errorCallback,
            Runnable successCallback) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                updateProgress(progressCallback, "Preparing export data...");
                
                List<DisplayEntry> allEntries = new ArrayList<>();

                // Add all work logs
                updateProgress(progressCallback, "Loading work logs...");
                List<RegistroTrabalho> allLogs = companyService.getWorkLogFileManager().getAllWorkLogs();
                for (RegistroTrabalho r : allLogs) {
                    allEntries.add(new DisplayEntry(r));
                }

                // Clear bill cache to ensure fresh data
                updateProgress(progressCallback, "Loading bills...");
                companyService.clearBillCache();

                // Add all bills
                for (List<Bill> monthlyBills : companyService.getAllBills().values()) {
                    allEntries.addAll(monthlyBills.stream().map(DisplayEntry::new).toList());
                }

                updateProgress(progressCallback, "Sorting data...");
                allEntries.sort(Comparator.comparing(DisplayEntry::getDate));
                
                updateProgress(progressCallback, "Generating Excel file...");
                boolean isAllExport = true;
                ExcelExporter.exportToExcel(allEntries, companyService, isAllExport);
                
                updateProgress(progressCallback, "Export completed successfully!");
                Platform.runLater(successCallback);
                
            } catch (Exception e) {
                ErrorHandler.handleUnexpectedError("Excel export", e);
                Platform.runLater(() -> errorCallback.accept(e));
            }
        });
    }
    
    /**
     * Exports filtered data to Excel asynchronously
     */
    public CompletableFuture<Void> exportFilteredToExcelAsync(
            String year, String month, String company,
            Consumer<String> progressCallback,
            Consumer<Exception> errorCallback,
            Runnable successCallback) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                updateProgress(progressCallback, "Preparing filtered export...");
                
                List<DisplayEntry> filteredEntries = new ArrayList<>();

                // Add filtered work logs
                updateProgress(progressCallback, "Loading filtered work logs...");
                List<RegistroTrabalho> filteredLogs = companyService.applyFilters(year, month, company);
                for (RegistroTrabalho r : filteredLogs) {
                    filteredEntries.add(new DisplayEntry(r));
                }

                // Add filtered bills if year/month filter is applied
                if (year != null && month != null) {
                    updateProgress(progressCallback, "Loading bills for period...");
                    String yearMonth = year + "-" + String.format("%02d", Integer.parseInt(month));
                    List<Bill> monthlyBills = companyService.getBillsForMonth(yearMonth);
                    filteredEntries.addAll(monthlyBills.stream().map(DisplayEntry::new).toList());
                }

                updateProgress(progressCallback, "Sorting filtered data...");
                filteredEntries.sort(Comparator.comparing(DisplayEntry::getDate));
                
                updateProgress(progressCallback, "Generating Excel file...");
                boolean isAllExport = false;
                ExcelExporter.exportToExcel(filteredEntries, companyService, isAllExport);
                
                updateProgress(progressCallback, "Filtered export completed successfully!");
                Platform.runLater(successCallback);
                
            } catch (Exception e) {
                ErrorHandler.handleUnexpectedError("filtered Excel export", e);
                Platform.runLater(() -> errorCallback.accept(e));
            }
        });
    }
    
    /**
     * Creates a JavaFX Task for Excel export with built-in progress tracking
     */
    public Task<Void> createExportTask(boolean exportAll, String year, String month, String company) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Initializing export...");
                updateProgress(0, 100);
                
                List<DisplayEntry> entries = new ArrayList<>();
                
                if (exportAll) {
                    updateMessage("Loading all work logs...");
                    updateProgress(20, 100);
                    
                    List<RegistroTrabalho> allLogs = companyService.getWorkLogFileManager().getAllWorkLogs();
                    for (RegistroTrabalho r : allLogs) {
                        entries.add(new DisplayEntry(r));
                    }
                    
                    updateMessage("Loading all bills...");
                    updateProgress(40, 100);
                    
                    companyService.clearBillCache();
                    for (List<Bill> monthlyBills : companyService.getAllBills().values()) {
                        entries.addAll(monthlyBills.stream().map(DisplayEntry::new).toList());
                    }
                } else {
                    updateMessage("Loading filtered data...");
                    updateProgress(20, 100);
                    
                    List<RegistroTrabalho> filteredLogs = companyService.applyFilters(year, month, company);
                    for (RegistroTrabalho r : filteredLogs) {
                        entries.add(new DisplayEntry(r));
                    }
                    
                    if (year != null && month != null) {
                        String yearMonth = year + "-" + String.format("%02d", Integer.parseInt(month));
                        List<Bill> monthlyBills = companyService.getBillsForMonth(yearMonth);
                        entries.addAll(monthlyBills.stream().map(DisplayEntry::new).toList());
                    }
                }
                
                updateMessage("Sorting data...");
                updateProgress(60, 100);
                entries.sort(Comparator.comparing(DisplayEntry::getDate));
                
                updateMessage("Generating Excel file...");
                updateProgress(80, 100);
                ExcelExporter.exportToExcel(entries, companyService, exportAll);
                
                updateMessage("Export completed!");
                updateProgress(100, 100);
                
                return null;
            }
        };
    }
    
    /**
     * Helper method to safely update progress on UI thread
     */
    private void updateProgress(Consumer<String> progressCallback, String message) {
        if (progressCallback != null) {
            Platform.runLater(() -> progressCallback.accept(message));
        }
    }
}