package com.example.worklogui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controller responsible for managing the work log table display
 */
public class LogTableController {

    @FXML private TableView<DisplayEntry> logTable;
    @FXML private TableColumn<DisplayEntry, String> dateCol;
    @FXML private TableColumn<DisplayEntry, String> companyCol;
    @FXML private TableColumn<DisplayEntry, Double> hoursCol;
    @FXML private TableColumn<DisplayEntry, Double> minutesCol;
    @FXML private TableColumn<DisplayEntry, Boolean> doublePayCol;
    @FXML private TableColumn<DisplayEntry, String> earningsCol;
    @FXML private Label netTotalLabel;

    private final CompanyManagerService service;
    private ObservableList<DisplayEntry> displayEntries = FXCollections.observableArrayList();
    private Consumer<String> statusMessageHandler;
    private FilterController filterController;


    public LogTableController(CompanyManagerService service) {
        this.service = service;
    }

    public void setFilterController(FilterController filterController) {
        this.filterController = filterController;
    }
    /**
     * Set up table columns and event handlers
     */
    public void initialize() {
        setupTableColumns();
        setupRowDoubleClick();
    }

    /**
     * Set callback for status messages
     */
    public void setStatusMessageHandler(Consumer<String> handler) {
        this.statusMessageHandler = handler;
    }

    /**
     * Set up the table controls
     */
    public void setupControls(TableView<DisplayEntry> logTable, TableColumn<DisplayEntry, String> dateCol,
                              TableColumn<DisplayEntry, String> companyCol, TableColumn<DisplayEntry, Double> hoursCol,
                              TableColumn<DisplayEntry, Double> minutesCol, TableColumn<DisplayEntry, Boolean> doublePayCol,
                              TableColumn<DisplayEntry, String> earningsCol, Label netTotalLabel) {
        this.logTable = logTable;
        this.dateCol = dateCol;
        this.companyCol = companyCol;
        this.hoursCol = hoursCol;
        this.minutesCol = minutesCol;
        this.doublePayCol = doublePayCol;
        this.earningsCol = earningsCol;
        this.netTotalLabel = netTotalLabel;

        initialize();
    }

    /**
     * Set up table columns with cell factories
     */
    private void setupTableColumns() {
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateFormatted()));
        companyCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLabel()));
        hoursCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getHoras()));
        minutesCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMinutos()));
        doublePayCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().isPagamentoDobrado()));
        earningsCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedEarnings(service)));
    }

    /**
     * Set up double-click handler for table rows
     */
    private void setupRowDoubleClick() {
        logTable.setRowFactory(tv -> {
            TableRow<DisplayEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && !row.getItem().isBill()) {
                    onEditLogEntry();
                }
            });
            return row;
        });
    }

    /**
     * Update table with new data based on filters
     */
    public void updateTable(String year, String month, String company) {
        String y = year;
        String m = month;
        String c = company;

        if ("All".equals(c)) c = null;
        if ("All".equals(m)) m = null;
        boolean allYears = "All".equals(y);

        List<DisplayEntry> combined = new ArrayList<>();
        List<RegistroTrabalho> filteredRegistros = new ArrayList<>();
        List<Bill> filteredBills = new ArrayList<>();

        // Clear bill cache to ensure fresh data
        service.clearBillCache();
        Map<String, List<String>> yearToMonthsMap = buildYearToMonthsMap();

        if (allYears) {
            for (String currentYear : yearToMonthsMap.keySet()) {
                List<RegistroTrabalho> logs = service.applyFilters(currentYear, m, c);
                filteredRegistros.addAll(logs);
                for (RegistroTrabalho r : logs) {
                    combined.add(new DisplayEntry(r));
                }

                for (String currentMonth : yearToMonthsMap.getOrDefault(currentYear, Collections.emptyList())) {
                    if (m == null || currentMonth.equals(m)) {
                        List<Bill> monthBills = service.getBillsForMonth(currentYear + "-" + currentMonth);
                        filteredBills.addAll(monthBills);
                        for (Bill b : monthBills) {
                            combined.add(new DisplayEntry(b));
                        }
                    }
                }
            }
        } else {
            List<RegistroTrabalho> logs = service.applyFilters(y, m, c);
            filteredRegistros.addAll(logs);
            for (RegistroTrabalho r : logs) {
                combined.add(new DisplayEntry(r));
            }

            if (y != null) {
                if (m != null) {
                    String ym = y + "-" + m;
                    List<Bill> monthBills = service.getBillsForMonth(ym);
                    filteredBills.addAll(monthBills);
                    for (Bill b : monthBills) {
                        combined.add(new DisplayEntry(b));
                    }
                } else {
                    for (String currentMonth : yearToMonthsMap.getOrDefault(y, Collections.emptyList())) {
                        String ym = y + "-" + currentMonth;
                        List<Bill> monthBills = service.getBillsForMonth(ym);
                        filteredBills.addAll(monthBills);
                        for (Bill b : monthBills) {
                            combined.add(new DisplayEntry(b));
                        }
                    }
                }
            }
        }

        updateTableWithEntries(combined);
        updateAGISummary(filteredRegistros, filteredBills);

        // Set status message based on results
        if (combined.isEmpty()) {
            setStatusMessage("ℹ No records match the selected filters.\nℹ Nenhum registro corresponde aos filtros selecionados.");
        } else {
            setStatusMessage("✓ Filters applied. Showing " + combined.size() + " records.\n✓ Filtros aplicados. Mostrando " + combined.size() + " registros.");
        }
    }

    /**
     * Update the table with the given entries
     */
    private void updateTableWithEntries(List<DisplayEntry> entries) {
        entries.sort(Comparator.comparing(DisplayEntry::getDate));
        displayEntries.setAll(entries);
        logTable.setItems(displayEntries);
        logTable.refresh();
    }

    /**
     * Update the AGI summary information
     */
    private void updateAGISummary(List<RegistroTrabalho> filteredRegistros, List<Bill> filteredBills) {
        try {
            // Calculate AGI using the calculator
            AGICalculator.AGIResult agiResult = AGICalculator.calculateAGI(filteredRegistros, filteredBills);

            // Calculate total bill amount (both deductible and non-deductible)
            double totalBillAmount = filteredBills.stream()
                    .mapToDouble(Bill::getAmount)
                    .sum();

            // Calculate non-deductible expenses
            double nonDeductibleExpenses = filteredBills.stream()
                    .filter(bill -> bill.getCategory() == null || !bill.getCategory().isDeductible())
                    .mapToDouble(Bill::getAmount)
                    .sum();

            // Calculate income after all bills (cash flow perspective)
            double incomeAfterAllBills = agiResult.grossIncome - totalBillAmount;

            // Create Detail Label including non-deductible expenses
            String detailText = String.format(
                    "📈 Gross: $%.2f | 💸 Deductible: $%.2f | 🚫 Non-deductible: $%.2f | 💵 After Bills: $%.2f | 📉 Net: $%.2f | 📊 NESE: $%.2f | 💰 AGI: $%.2f",
                    agiResult.grossIncome,
                    agiResult.businessExpenses,
                    nonDeductibleExpenses,
                    incomeAfterAllBills,
                    agiResult.netEarnings,
                    agiResult.nese,
                    agiResult.adjustedGrossIncome
            );
            netTotalLabel.setText(detailText);
        } catch (Exception e) {
            System.err.println("Error updating AGI summary: " + e.getMessage());
            e.printStackTrace();
            netTotalLabel.setText("Error calculating summary: " + e.getMessage());
        }
    }

    /**
     * Scroll to the most recently added bill
     */
    public void scrollToMostRecentBill() {
        if (logTable.getItems().isEmpty()) return;

        DisplayEntry newestBill = null;
        for (DisplayEntry entry : logTable.getItems()) {
            if (entry.isBill()) {
                if (newestBill == null || entry.getDate().isAfter(newestBill.getDate())) {
                    newestBill = entry;
                }
            }
        }

        if (newestBill != null) {
            logTable.getSelectionModel().select(newestBill);
            logTable.scrollTo(newestBill);
        }
    }

    public void onEditLogEntry() {
        DisplayEntry selected = logTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBill()) {
            setStatusMessage("⚠ No record selected.\n⚠ Nenhum registro selecionado.");
            return;
        }

        // Open log editor with current filter settings
        LogEditorUI editor = new LogEditorUI();
        editor.setOnClose(() -> {
            try {
                service.reloadRegistros();
                // Refresh table (will be called by main controller)
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Pass the current filter values

        String year = filterController.getSelectedYear();
        String month = filterController.getSelectedMonth();
        String company = filterController.getSelectedCompany();

        editor.show((Stage) logTable.getScene().getWindow(), year, month, company);
    }

    // Solution:
    public void onDeleteLogEntry() {
        DisplayEntry selected = logTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBill()) {
            setStatusMessage("⚠ No record selected.\n⚠ Nenhum registro selecionado.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this entry?\nTem certeza que deseja excluir este registro?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // Delete the registro from the service
                    service.deleteRegistro(selected.getRegistro());

                    // Manually remove the item from the displayEntries list
                    displayEntries.remove(selected);

                    // Refresh the table display
                    logTable.refresh();

                    setStatusMessage("✔ Entry deleted.\n✔ Registro excluído.");

                    // Update the AGI summary
                    String year = filterController.getSelectedYear();
                    String month = filterController.getSelectedMonth();
                    String company = filterController.getSelectedCompany();

                    // Get fresh data from the service
                    List<RegistroTrabalho> filteredRegistros = service.applyFilters(year, month, company);
                    List<Bill> filteredBills = new ArrayList<>();

                    // Get bills for the current filters
                    if (!"All".equals(year) && !"All".equals(month)) {
                        String ym = year + "-" + month;
                        filteredBills = service.getBillsForMonth(ym);
                    }

                    // Update AGI summary
                    updateAGISummary(filteredRegistros, filteredBills);

                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Delete Error",
                            "Failed to delete entry.\nFalha ao excluir registro.\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Clear the table display
     */
    public void clearDisplay() {
        logTable.setItems(FXCollections.observableArrayList());
        netTotalLabel.setText("📉 Net Total: —");
    }

    /**
     * Get all entries currently displayed in the table
     */
    public List<DisplayEntry> getCurrentDisplayEntries() {
        return new ArrayList<>(displayEntries);
    }

    /**
     * Helper method to build year-to-months map
     */
    private Map<String, List<String>> buildYearToMonthsMap() {
        Map<String, List<String>> result = new HashMap<>();
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

        // Add bills
        Map<String, List<Bill>> allBills = service.getAllBills();
        for (Map.Entry<String, List<Bill>> entry : allBills.entrySet()) {
            for (Bill bill : entry.getValue()) {
                LocalDate date = bill.getDate();
                String year = String.valueOf(date.getYear());
                String month = String.format("%02d", date.getMonthValue());

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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}