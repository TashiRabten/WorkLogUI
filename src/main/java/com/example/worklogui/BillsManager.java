package com.example.worklogui;

import com.example.worklogui.utils.CalculationUtils;
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
    private String lastEditedYear;
    private String lastEditedMonth;



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

    public String getLastEditedYear() { return lastEditedYear; }
    public String getLastEditedMonth() { return lastEditedMonth; }

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
        List<Bill> allBills = collectFilteredBills(year, month, company);
        String title = buildDialogTitle(year, month);
        
        new BillsEditorUI("Multiple", allBills, service,
                parentStage, this::filterToEditedBill).show();
    }

    private List<Bill> collectFilteredBills(String year, String month, String company) {
        List<Bill> allBills = new ArrayList<>();
        Map<String, List<String>> yearToMonthsMap = buildYearToMonthsMap();
        List<String> yearsToInclude = determineYearsToInclude(year, yearToMonthsMap);

        for (String y : yearsToInclude) {
            List<String> monthsToInclude = determineMonthsToInclude(month, y, yearToMonthsMap);
            collectBillsForYearMonths(y, monthsToInclude, company, allBills);
        }
        
        return allBills;
    }

    private List<String> determineYearsToInclude(String year, Map<String, List<String>> yearToMonthsMap) {
        List<String> yearsToInclude = new ArrayList<>();
        if ("All".equals(year)) {
            yearsToInclude.addAll(yearToMonthsMap.keySet());
        } else {
            yearsToInclude.add(year);
        }
        return yearsToInclude;
    }

    private List<String> determineMonthsToInclude(String month, String year, Map<String, List<String>> yearToMonthsMap) {
        if ("All".equals(month)) {
            return yearToMonthsMap.getOrDefault(year, Collections.emptyList());
        } else {
            List<String> monthsToInclude = new ArrayList<>();
            if (yearToMonthsMap.getOrDefault(year, Collections.emptyList()).contains(month)) {
                monthsToInclude.add(month);
            }
            return monthsToInclude;
        }
    }

    private void collectBillsForYearMonths(String year, List<String> months, String company, List<Bill> allBills) {
        for (String month : months) {
            String yearMonth = year + "-" + month;
            List<Bill> monthBills = service.getBillsForMonth(yearMonth);
            List<Bill> filteredBills = applyCompanyFilter(monthBills, company);
            allBills.addAll(filteredBills);
        }
    }

    private List<Bill> applyCompanyFilter(List<Bill> bills, String company) {
        if ("All".equals(company)) {
            return bills;
        }
        
        List<Bill> filteredBills = new ArrayList<>();
        for (Bill bill : bills) {
            if (company.equals(bill.getLabel())) {
                filteredBills.add(bill);
            }
        }
        return filteredBills;
    }

    private String buildDialogTitle(String year, String month) {
        String title = "All".equals(year) ? "All Years" : "Year: " + year;
        title += "All".equals(month) ? ", All Months" : ", Month: " + month;
        return title;
    }


    private void filterToEditedBill(String editedYear, String editedMonth) {
        try {
            service.clearBillCache();

            // If we have a valid year and month from the edited bill, notify callback
            if (editedYear != null && editedMonth != null) {
                validateBillCategories(editedYear, editedMonth);
                setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");

                // IMPORTANT: Store the edited year/month so refreshAfterBillsUpdated can use it
                lastEditedYear = editedYear;
                lastEditedMonth = editedMonth;
            } else {
                setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");
            }

            // Execute callback to refresh UI
            if (onBillsUpdatedCallback != null) {
                onBillsUpdatedCallback.run();
            }
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
        return CalculationUtils.buildYearToMonthsMap(service.getRegistros(), service.getAllBills());
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