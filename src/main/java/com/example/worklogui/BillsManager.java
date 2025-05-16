package com.example.worklogui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Manages bill-related operations and UI
 */
public class BillsManager {

    private final CompanyManagerService service;
    private Consumer<String> statusMessageHandler;
    private Consumer<String> warningMessageHandler;
    private Runnable onBillsUpdatedCallback;

    public BillsManager(CompanyManagerService service) {
        this.service = service;
    }

    /**
     * Set callback for status messages
     */
    public void setStatusMessageHandler(Consumer<String> handler) {
        this.statusMessageHandler = handler;
    }

    /**
     * Set callback for warning messages
     */
    public void setWarningMessageHandler(Consumer<String> handler) {
        this.warningMessageHandler = handler;
    }

    /**
     * Set callback for when bills are updated
     */
    public void setOnBillsUpdatedCallback(Runnable callback) {
        this.onBillsUpdatedCallback = callback;
    }

    /**
     * Edit bills for the specified filters
     */
    public void editBills(Stage parentStage, String year, String month, String company) {
        if (year == null || month == null) {
            setStatusMessage("⚠ Please select both year and month to edit bills.\n⚠ Por favor, selecione ano e mês.");
            return;
        }

        service.clearBillCache(); // Clear cache before loading bills

        if ("All".equals(year) || "All".equals(month)) {
            editAllFilteredBills(parentStage, year, month, company);
        } else {
            editSpecificMonthBills(parentStage, year, month, company);
        }
    }

    /**
     * Edit bills for a specific year-month combination
     */
    private void editSpecificMonthBills(Stage parentStage, String year, String month, String company) {
        String yearMonthKey = year + "-" + month;
        List<Bill> monthBills = service.getBillsForMonth(yearMonthKey);

        // Filter by company if needed
        if (!"All".equals(company)) {
            List<Bill> filteredBills = new ArrayList<>();
            for (Bill b : monthBills) {
                if (company.equals(b.getLabel())) {
                    filteredBills.add(b);
                }
            }
            monthBills = filteredBills;
        }

        // Use the constructor that takes a BiConsumer
        new BillsEditorUI(yearMonthKey, monthBills, service,
                parentStage, this::filterToEditedBill).show();
    }

    /**
     * Edit all bills matching the filter criteria
     */
    private void editAllFilteredBills(Stage parentStage, String year, String month, String company) {
        // Get all bills and filter based on dropdown selections
        List<Bill> allBills = new ArrayList<>();
        Map<String, List<String>> yearToMonthsMap = buildYearToMonthsMap();

        // Determine which years to include
        List<String> yearsToInclude = new ArrayList<>();
        if ("All".equals(year)) {
            yearsToInclude.addAll(yearToMonthsMap.keySet());
        } else {
            yearsToInclude.add(year);
        }

        // Loop through years and months
        for (String y : yearsToInclude) {
            List<String> monthsToInclude;
            if ("All".equals(month)) {
                monthsToInclude = yearToMonthsMap.getOrDefault(y, Collections.emptyList());
            } else {
                monthsToInclude = new ArrayList<>();
                if (yearToMonthsMap.getOrDefault(y, Collections.emptyList()).contains(month)) {
                    monthsToInclude.add(month);
                }
            }

            for (String m : monthsToInclude) {
                String ym = y + "-" + m;
                List<Bill> monthBills = service.getBillsForMonth(ym);

                // Apply company filter if needed
                if (!"All".equals(company)) {
                    List<Bill> filteredBills = new ArrayList<>();
                    for (Bill b : monthBills) {
                        if (company.equals(b.getLabel())) {
                            filteredBills.add(b);
                        }
                    }
                    monthBills = filteredBills;
                }

                allBills.addAll(monthBills);
            }
        }

        String title = "All".equals(year) ? "All Years" : "Year: " + year;
        title += "All".equals(month) ? ", All Months" : ", Month: " + month;

        // Use the constructor that takes a BiConsumer
        new BillsEditorUI("Multiple", allBills, service,
                parentStage, this::filterToEditedBill).show();
    }

    /**
     * Handle callback when bills are edited, to filter to the edited bill's date
     */
    private void filterToEditedBill(String editedYear, String editedMonth) {
        try {
            service.clearBillCache();

            // If we have a valid year and month from the edited bill, notify callback
            if (editedYear != null && editedMonth != null) {
                // Ensure all bills have valid categories before saving
                validateBillCategories(editedYear, editedMonth);

                setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");

                // Check if this is the current month for warnings
                LocalDate now = LocalDate.now();
                String currentYear = String.valueOf(now.getYear());
                String currentMonth = String.format("%02d", now.getMonthValue());

                if ((editedYear.equals(currentYear)) && (editedMonth.equals(currentMonth))) {
                    String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros());
                    if (warning != null) {
                        setWarningMessage(WarningUtils.appendTimestampedWarning(warning));
                    }
                }
            } else {
                setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");
            }

            // Execute callback to refresh UI
            if (onBillsUpdatedCallback != null) {
                onBillsUpdatedCallback.run();
            }

            // Scroll to most recent bill (will be done by main controller)
        } catch (Exception ex) {
            setStatusMessage("❌ Error updating bills: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Validate and fix any bills with missing categories
     */
    private void validateBillCategories(String year, String month) {
        try {
            String yearMonth = year + "-" + month;
            List<Bill> bills = service.getBillsForMonth(yearMonth);
            boolean needsUpdate = false;

            for (Bill bill : bills) {
                if (bill.getCategory() == null) {
                    // Fix bills with null categories
                    bill.setCategory(ExpenseCategory.NONE);
                    needsUpdate = true;
                }
            }

            if (needsUpdate) {
                service.setBillsForMonth(yearMonth, bills);
                System.out.println("Fixed bills with missing categories in " + yearMonth);
            }
        } catch (Exception e) {
            System.err.println("Error validating bill categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to build year-to-months map
     */
    private Map<String, List<String>> buildYearToMonthsMap() {
        Map<String, List<String>> result = new HashMap<>();

        // First, add all years/months from work logs
        for (RegistroTrabalho r : service.getRegistros()) {
            try {
                LocalDate date = LocalDate.parse(r.getData(), java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                String year = String.valueOf(date.getYear());
                String month = String.format("%02d", date.getMonthValue());

                List<String> months = result.computeIfAbsent(year, k -> new ArrayList<>());
                if (!months.contains(month)) {
                    months.add(month);
                    Collections.sort(months);
                }
            } catch (Exception ignored) {}
        }

        // Then add all years/months from bills
        Map<String, List<Bill>> allBills = service.getAllBills();
        for (String yearMonth : allBills.keySet()) {
            if (yearMonth.length() >= 7) {  // Format YYYY-MM
                String year = yearMonth.substring(0, 4);
                String month = yearMonth.substring(5, 7);

                List<String> months = result.computeIfAbsent(year, k -> new ArrayList<>());
                if (!months.contains(month)) {
                    months.add(month);
                    Collections.sort(months);
                }
            }
        }

        return result;
    }

    private void setStatusMessage(String message) {
        if (statusMessageHandler != null) {
            statusMessageHandler.accept(message);
        }
    }

    private void setWarningMessage(String warning) {
        if (warningMessageHandler != null) {
            warningMessageHandler.accept(warning);
        }
    }
}