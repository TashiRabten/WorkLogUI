package com.example.worklogui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controller responsible for handling filter operations
 * (the bottom section of the main UI with year, month, and company filters)
 */
public class FilterController {

    @FXML private ComboBox<String> yearFilter;
    @FXML private ComboBox<String> monthFilter;
    @FXML private ComboBox<String> companyFilter;
    
    private final CompanyManagerService service;
    private Consumer<String> statusMessageHandler;
    private Runnable onFilterAppliedCallback;
    
    private Map<String, List<String>> yearToMonthsMap = new HashMap<>();
    
    public FilterController(CompanyManagerService service) {
        this.service = service;
    }
    
    /**
     * Initialize filters with current date selected
     */
    public void initialize() {
        refreshYearToMonthsMap();
        setupFilters();
    }
    
    /**
     * Set up the filter controls
     */
    public void setupControls(ComboBox<String> yearFilter, ComboBox<String> monthFilter, 
                             ComboBox<String> companyFilter) {
        this.yearFilter = yearFilter;
        this.monthFilter = monthFilter;
        this.companyFilter = companyFilter;
        
        // Set up event handlers
        if (yearFilter != null) {
            yearFilter.setOnAction(e -> updateMonthFilter());
        }
        
        initialize();
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
        // Warning message handler removed as it was unused
    }
    
    /**
     * Set callback for when filters are applied
     */
    public void setOnFilterAppliedCallback(Runnable callback) {
        this.onFilterAppliedCallback = callback;
    }
    
    /**
     * Refresh the year-to-month mapping based on available data
     */
    public void refreshYearToMonthsMap() {
        yearToMonthsMap.clear();
        for (RegistroTrabalho r : service.getRegistros()) {
            updateYearToMonthsMapFromEntry(r);
        }
        
        // Clear the cache before loading bills
        service.clearBillCache();
        loadBillsIntoYearMonthMap();
    }
    
    /**
     * Update month dropdown based on selected year
     */
    public void updateMonthFilter() {
        String selectedYear = yearFilter.getValue();
        if (selectedYear == null || "All".equals(selectedYear) || !yearToMonthsMap.containsKey(selectedYear)) {
            monthFilter.setItems(FXCollections.observableArrayList("All"));
            monthFilter.setValue("All");
            return;
        }
        
        List<String> availableMonths = new ArrayList<>(yearToMonthsMap.get(selectedYear));
        Collections.sort(availableMonths);
        List<String> months = new ArrayList<>();
        months.add("All");
        months.addAll(availableMonths);
        monthFilter.setItems(FXCollections.observableArrayList(months));
        
        if (!months.contains(monthFilter.getValue())) {
            monthFilter.setValue("All");
        }
    }
    
    /**
     * Update year filter dropdown items
     */
    public void updateYearFilterItems() {
        List<String> allYears = new ArrayList<>(yearToMonthsMap.keySet());
        Collections.sort(allYears);
        allYears.add(0, "All");

        String currentSelection = yearFilter.getValue();
        yearFilter.setItems(FXCollections.observableArrayList(allYears));

        // Maintain selected item if it still exists
        if (allYears.contains(currentSelection)) {
            yearFilter.setValue(currentSelection);
        } else {
            yearFilter.setValue("All");
        }
    }
    
    /**
     * Set up filters with available data
     */
    private void setupFilters() {
        updateYearFilterItems();

        List<String> companies = new ArrayList<>(service.getCompanies());
        Collections.sort(companies);
        companies.add(0, "All");
        companyFilter.setItems(FXCollections.observableArrayList(companies));
        companyFilter.setValue("All");

        // Set to current year/month
        yearFilter.setValue(String.valueOf(LocalDate.now().getYear()));
        updateMonthFilter();
        monthFilter.setValue(String.format("%02d", LocalDate.now().getMonthValue()));
    }
    
    /**
     * Apply the current filter selections
     */
    public void onApplyFilter() {
        if (onFilterAppliedCallback != null) {
            onFilterAppliedCallback.run();
        } else {
        }
    }
    
    /**
     * Clear the filter display but keep filter selections
     */
    public void onClearFilter() {
        setStatusMessage("✔ Display cleared. Filters unchanged.\n✔ Tela limpa. Filtros mantidos.");
    }
    
    /**
     * Get the currently selected year filter
     */
    public String getSelectedYear() {
        return yearFilter.getValue();
    }
    
    /**
     * Get the currently selected month filter
     */
    public String getSelectedMonth() {
        return monthFilter.getValue(); 
    }
    
    /**
     * Get the currently selected company filter
     */
    public String getSelectedCompany() {
        return companyFilter.getValue();
    }

    /**
     * Set the filter values programmatically
     */
    public void setFilterValues(String year, String month, String company) {
        if (year != null) {
            yearFilter.setValue(year);
            updateMonthFilter();
        }
        
        if (month != null) {
            monthFilter.setValue(month);
        }
        
        if (company != null) {
            companyFilter.setValue(company);
        }
    }
    
    /**
     * Helper method to update year-to-months map from bills
     */
    private void loadBillsIntoYearMonthMap() {
        service.clearBillCache();
        Map<String, List<Bill>> allBills = service.getAllBills();
        for (Map.Entry<String, List<Bill>> entry : allBills.entrySet()) {
            updateYearToMonthsMapFromBillList(entry.getValue());
        }
    }
    
    /**
     * Helper method to update year-to-months map from bills
     */
    private void updateYearToMonthsMapFromBillList(List<Bill> billList) {
        for (Bill bill : billList) {
            LocalDate date = bill.getDate();
            String year = String.valueOf(date.getYear());
            String month = String.format("%02d", date.getMonthValue());

            List<String> months = yearToMonthsMap.computeIfAbsent(year, k -> new ArrayList<>());
            if (!months.contains(month)) {
                months.add(month);
                Collections.sort(months);
            }
        }
    }
    
    /**
     * Helper method to update year-to-months map from work entries
     */
    private void updateYearToMonthsMapFromEntry(RegistroTrabalho r) {
        try {
            LocalDate date = LocalDate.parse(r.getData(), java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            String year = String.valueOf(date.getYear());
            String month = String.format("%02d", date.getMonthValue());

            List<String> months = yearToMonthsMap.computeIfAbsent(year, k -> new ArrayList<>());
            if (!months.contains(month)) {
                months.add(month);
                Collections.sort(months);
            }
        } catch (Exception ignored) {}
    }
    
    private void setStatusMessage(String message) {
        if (statusMessageHandler != null) {
            statusMessageHandler.accept(message);
        }
    }
}
