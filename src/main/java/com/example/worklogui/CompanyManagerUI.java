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

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;

public class CompanyManagerUI {

    @FXML private TextField dateField;
    @FXML private ComboBox<String> jobTypeCombo;
    @FXML private TextField valueField;
    @FXML private CheckBox doublePayCheckBox;
    @FXML private TextArea statusArea;
    @FXML private Button editCompaniesBtn;
    @FXML private Button openLogEditorBtn;
    @FXML private Button editBillsBtn;
    @FXML private Label netTotalLabel;

    @FXML private ComboBox<String> yearFilter;
    @FXML private ComboBox<String> monthFilter;
    @FXML private ComboBox<String> companyFilter;

    @FXML private TableView<DisplayEntry> logTable;
    @FXML private TableColumn<DisplayEntry, String> dateCol;
    @FXML private TableColumn<DisplayEntry, String> companyCol;
    @FXML private TableColumn<DisplayEntry, Double> hoursCol;
    @FXML private TableColumn<DisplayEntry, Double> minutesCol;
    @FXML private TableColumn<DisplayEntry, Boolean> doublePayCol;
    @FXML private TableColumn<DisplayEntry, String> earningsCol;

    private final CompanyManagerService service = new CompanyManagerService();
    private ObservableList<DisplayEntry> displayEntries = FXCollections.observableArrayList();
    private Map<String, List<String>> yearToMonthsMap = new HashMap<>();

    // Track current warning and status message
    private String currentWarning = null;
    private String statusMessage = null;

    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            service.initialize();
            refreshYearToMonthsMap();
            setupFilters();
            jobTypeCombo.setItems(FXCollections.observableArrayList(CompanyRateService.getInstance().getRates().keySet()));
            reloadCompanyList();
            onApplyFilter();
            setupRowDoubleClick();
        } catch (Exception e) {
            setStatusMessage("Erro ao inicializar / Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshYearToMonthsMap() {
        yearToMonthsMap.clear();
        for (RegistroTrabalho r : service.getRegistros()) {
            updateYearToMonthsMapFromEntry(r);
        }
        // Clear the cache before loading bills
        service.clearBillCache();
        loadBillsIntoYearMonthMap();
    }

    private double sumBillsForYearMonth(String year, String month, String company) {
        String yearMonth = year + "-" + month;
        return service.getBillsForMonth(yearMonth).stream()
                .filter(b -> company == null || b.getLabel().equals(company))
                .mapToDouble(Bill::getAmount)
                .sum();
    }

    private void loadBillsForYearMonth(String year, String month, List<DisplayEntry> combined, String company) {
        String yearMonth = year + "-" + month;
        for (Bill b : service.getBillsForMonth(yearMonth)) {
            // Filter by company if needed
            if (company == null || b.getLabel().equals(company)) {
                combined.add(new DisplayEntry(b));
            }
        }
    }

    private void loadBillsIntoYearMonthMap() {
        service.clearBillCache(); // Clear bill cache to ensure fresh data
        Map<String, List<Bill>> allBills = service.getAllBills();
        for (Map.Entry<String, List<Bill>> entry : allBills.entrySet()) {
            updateYearToMonthsMapFromBillList(entry.getValue());
        }
    }

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

    private void updateYearToMonthsMapFromEntry(RegistroTrabalho r) {
        try {
            LocalDate date = LocalDate.parse(r.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            String year = String.valueOf(date.getYear());
            String month = String.format("%02d", date.getMonthValue());

            List<String> months = yearToMonthsMap.computeIfAbsent(year, k -> new ArrayList<>());
            if (!months.contains(month)) {
                months.add(month);
                Collections.sort(months);
            }
        } catch (Exception ignored) {}
    }

    private void scrollToMostRecentBill() {
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

    private void updateMonthFilter() {
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

    private void setupTableColumns() {
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateFormatted()));
        companyCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLabel()));
        hoursCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getHoras()));
        minutesCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMinutos()));
        doublePayCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().isPagamentoDobrado()));
        earningsCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedEarnings(service)));
    }

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

    private void setupFilters() {
        updateYearFilterItems();

        List<String> companies = new ArrayList<>(service.getCompanies());
        Collections.sort(companies);
        companies.add(0, "All");
        companyFilter.setItems(FXCollections.observableArrayList(companies));
        companyFilter.setValue("All");

        yearFilter.setValue(String.valueOf(LocalDate.now().getYear()));
        updateMonthFilter();
        monthFilter.setValue(String.format("%02d", LocalDate.now().getMonthValue()));

        yearFilter.setOnAction(e -> updateMonthFilter());
    }

    private void reloadCompanyList() {
        List<String> companies = new ArrayList<>(CompanyRateService.getInstance().getRates().keySet());
        Collections.sort(companies);
        companies.add(0, "All");
        companyFilter.setItems(FXCollections.observableArrayList(companies));
        if (!companyFilter.getItems().contains(companyFilter.getValue())) {
            companyFilter.setValue("All");
        }
        jobTypeCombo.setItems(FXCollections.observableArrayList(
                CompanyRateService.getInstance().getRates().keySet()));
    }


    private void updateStatusArea() {
        StringBuilder content = new StringBuilder();

        // Add status message if available
        if (statusMessage != null && !statusMessage.isEmpty()) {
            content.append(statusMessage);
        }

        // Add warning if available
        if (currentWarning != null && !currentWarning.isEmpty()) {
            // If we already have a status message, add some spacing
            if (content.length() > 0) {
                content.append("\n\n");
            }
            content.append(currentWarning);
        }

        // Update the status area
        statusArea.setText(content.toString());
    }

    private void setStatusMessage(String message) {
        this.statusMessage = message;
        updateStatusArea();
    }

    private void setWarning(String warning) {
        this.currentWarning = warning;
        updateStatusArea();
    }

    private void clearWarning() {
        this.currentWarning = null;
        updateStatusArea();
    }

    @FXML
    public void handleEditCompanies() {
        CompanyEditorUI editor = new CompanyEditorUI();
        editor.setOnClose(this::reloadCompanyList);
        editor.show((Stage) editCompaniesBtn.getScene().getWindow());
    }

    @FXML
    public void onEditBills() {
        String year = yearFilter.getValue();
        String month = monthFilter.getValue();
        String company = companyFilter.getValue();

        if (year == null || month == null) {
            setStatusMessage("⚠ Please select both year and month to edit bills.\n⚠ Por favor, selecione ano e mês.");
            return;
        }

        service.clearBillCache(); // Clear cache before loading bills

        if ("All".equals(year) || "All".equals(month)) {
            // Get all bills and filter based on dropdown selections
            List<Bill> allBills = new ArrayList<>();

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

            // Use the new constructor that takes a BiConsumer
            new BillsEditorUI("Multiple", allBills, service,
                    (Stage) editBillsBtn.getScene().getWindow(), this::filterToEditedBill).show();
        } else {
            // Specific year and month
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

            // Use the new constructor that takes a BiConsumer
            new BillsEditorUI(yearMonthKey, monthBills, service,
                    (Stage) editBillsBtn.getScene().getWindow(), this::filterToEditedBill).show();
        }
    }

    // Add this new method to handle filtering to the edited bill's date
    private void filterToEditedBill(String editedYear, String editedMonth) {
        try {
            service.clearBillCache();
            refreshYearToMonthsMap();
            updateYearFilterItems();

            // If we have a valid year and month from the edited bill, set the filters
            if (editedYear != null && editedMonth != null) {
                yearFilter.setValue(editedYear);
                updateMonthFilter();
                monthFilter.setValue(editedMonth);
            }

            onApplyFilter();

            // Check for warnings if edited the current month
            LocalDate now = LocalDate.now();
            String currentYear = String.valueOf(now.getYear());
            String currentMonth = String.format("%02d", now.getMonthValue());

            if ((editedYear == null || editedYear.equals(currentYear)) &&
                    (editedMonth == null || editedMonth.equals(currentMonth))) {
                String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros());
                if (warning != null) {
                    setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");
                    setWarning(WarningUtils.appendTimestampedWarning(warning));
                } else {
                    setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");
                    clearWarning();
                }
            } else {
                setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");
            }

            Platform.runLater(this::scrollToMostRecentBill);
        } catch (Exception ex) {
            setStatusMessage("❌ Error updating bills: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Helper method to refresh after bills are edited
    private void refreshAfterBillsEdit() {
        try {
            service.clearBillCache();
            refreshYearToMonthsMap();
            updateYearFilterItems();
            updateMonthFilter();
            onApplyFilter();
            setStatusMessage("✓ Bills updated.\n✓ Contas atualizadas.");
            Platform.runLater(this::scrollToMostRecentBill);
        } catch (Exception ex) {
            setStatusMessage("❌ Error updating bills: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    public void onApplyFilter() {
        String selectedYear = yearFilter.getValue();
        String selectedMonth = monthFilter.getValue();
        String selectedCompany = companyFilter.getValue();

        String y = selectedYear;
        String m = selectedMonth;
        String c = selectedCompany;

        if ("All".equals(c)) c = null;
        if ("All".equals(m)) m = null;
        boolean allYears = "All".equals(y);

        List<DisplayEntry> combined = new ArrayList<>();
        double grossTotal = 0.0;
        double billTotal = 0.0;

        // Clear bill cache to ensure fresh data
        service.clearBillCache();

        if (allYears) {
            for (String year : yearToMonthsMap.keySet()) {
                List<RegistroTrabalho> logs = service.applyFilters(year, m, c);
                for (RegistroTrabalho r : logs) {
                    combined.add(new DisplayEntry(r));
                    grossTotal += service.calculateEarnings(r);
                }

                for (String month : yearToMonthsMap.getOrDefault(year, Collections.emptyList())) {
                    // Only include this month if month filter is null or matches
                    if (m == null || month.equals(m)) {
                        for (Bill b : service.getBillsForMonth(year + "-" + month)) {
                            combined.add(new DisplayEntry(b));
                            billTotal += b.getAmount();
                        }
                    }
                }
            }
        } else {
            List<RegistroTrabalho> logs = service.applyFilters(y, m, c);
            for (RegistroTrabalho r : logs) {
                combined.add(new DisplayEntry(r));
                grossTotal += service.calculateEarnings(r);
            }

            if (y != null) {
                if (m != null) {
                    // Specific year and month
                    String ym = y + "-" + m;
                    for (Bill b : service.getBillsForMonth(ym)) {
                        combined.add(new DisplayEntry(b));
                        billTotal += b.getAmount();
                    }
                } else {
                    // Specific year, all months
                    for (String month : yearToMonthsMap.getOrDefault(y, Collections.emptyList())) {
                        String ym = y + "-" + month;
                        for (Bill b : service.getBillsForMonth(ym)) {
                            combined.add(new DisplayEntry(b));
                            billTotal += b.getAmount();
                        }
                    }
                }
            }
        }

        updateTable(combined);
        logTable.refresh();
        double net = grossTotal - billTotal;

        netTotalLabel.setText(String.format("📈 Gross: $%.2f    💸 Bills: $%.2f    📉 Net Total: $%.2f", grossTotal, billTotal, net));

        // Set the status message but preserve warnings
        setStatusMessage(String.format("✔ Filter applied. Showing %d entries.", combined.size()));
        if (!allYears && m != null) {
            String warning = WarningUtils.generateFilteredWarning(service.getRegistros(), selectedYear, selectedMonth);
            if (warning != null) {
                setWarning(warning);

                // Show popup for filtered month if needed
                javafx.application.Platform.runLater(() -> {
                    WarningUtils.showFilteredPopupWarningIfNeeded(service.getRegistros(), selectedYear, selectedMonth);
                });
            } else {
                clearWarning();
            }
        } else {
            // If we're showing all data, use the current month warning instead
            String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros());
            if (warning != null) {
                setWarning(warning);
            } else {
                clearWarning();
            }
        }
}



    private void updateTable(List<DisplayEntry> entries) {
        entries.sort(Comparator.comparing(DisplayEntry::getDate));
        displayEntries.setAll(entries);
        logTable.setItems(displayEntries);
    }

    private void updateYearFilterItems() {
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

    @FXML
    public void onLogWork() {
        try {
            String rawDate = dateField.getText().trim();
            String empresa = jobTypeCombo.getValue();
            String rawValue = valueField.getText().trim();
            boolean dobro = doublePayCheckBox.isSelected();

            if (rawDate.isEmpty() || empresa == null || rawValue.isEmpty()) {
                setStatusMessage("All fields are required.\nTodos os campos são obrigatórios.");
                return;
            }

            LocalDate parsedDate = DateParser.parseDate(rawDate);
            double valor = Double.parseDouble(rawValue);
            RegistroTrabalho newEntry = service.logWork(parsedDate, empresa, valor, dobro);

            // Refresh the year/month map from scratch
            refreshYearToMonthsMap();

            // Get the new year and month values for UI update
            String newYear = String.valueOf(parsedDate.getYear());
            String newMonth = String.format("%02d", parsedDate.getMonthValue());

            // Update filter dropdowns
            updateYearFilterItems();
            yearFilter.setValue(newYear);
            updateMonthFilter();
            monthFilter.setValue(newMonth);

            // Set company filter to match the new entry's company if not already set
            if (!"All".equals(companyFilter.getValue()) && !empresa.equals(companyFilter.getValue())) {
                companyFilter.setValue(empresa);
            }

            dateField.clear();
            valueField.clear();
            doublePayCheckBox.setSelected(false);

            String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros());
            if (warning != null) {
                setStatusMessage("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");
                setWarning(warning);

                // Reset the tracked month to ensure filter popups show for new data
                WarningUtils.resetTrackedMonth();
            } else {
                setStatusMessage("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");
                clearWarning();
            }

            onApplyFilter();  // shows entry with the correct filter
        } catch (Exception e) {
            setStatusMessage("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onClearFilter() {
        // Clear displayed results
        logTable.setItems(FXCollections.observableArrayList());
        setStatusMessage("✔ Display cleared. Filters unchanged.\n✔ Tela limpa. Filtros mantidos.");
        netTotalLabel.setText("📉 Net Total: —");
        // Keep existing warnings
    }

    @FXML
    public void onOpenLogEditor() {
        LogEditorUI editor = new LogEditorUI();
        editor.setOnClose(() -> {
            try {
                service.reloadRegistros();
                refreshYearToMonthsMap();

                // Update filter dropdowns
                updateYearFilterItems();
                updateMonthFilter();
            } catch (Exception e) {
                e.printStackTrace();
            }
            onApplyFilter();
        });
        editor.show((Stage) openLogEditorBtn.getScene().getWindow());
    }

    @FXML
    public void onEditLogEntry() {
        DisplayEntry selected = logTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBill()) {
            setStatusMessage("⚠ No record selected.\n⚠ Nenhum registro selecionado.");
            return;
        }
        onOpenLogEditor();
    }

    @FXML
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
                    // Check if this is for the current month
                    LocalDate entryDate = selected.getDate();
                    LocalDate now = LocalDate.now();
                    boolean isCurrentMonth = entryDate.getYear() == now.getYear() &&
                            entryDate.getMonthValue() == now.getMonthValue();

                    service.deleteRegistro(selected.getRegistro());
                    refreshYearToMonthsMap();
                    updateYearFilterItems();
                    updateMonthFilter();
                    onApplyFilter();

                    // Show warning if the deleted entry was from the current month
                    if (isCurrentMonth) {
                        String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros());
                        if (warning != null) {
                            setStatusMessage("✔ Entry deleted.\n✔ Registro excluído.");
                            setWarning(warning);
                            return;
                        }
                    }

                    setStatusMessage("✔ Entry deleted.\n✔ Registro excluído.");
                    // Keep any existing warnings that might still apply

                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Delete Error",
                            "Failed to delete entry.\nFalha ao excluir registro.\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void onShowTotal() {
        // Save existing warning
        String savedWarning = currentWarning;
        setStatusMessage(service.calculateTimeTotal());
        // Restore the warning
        currentWarning = savedWarning;
        updateStatusArea();
    }

    @FXML
    public void onShowEarnings() {
        // Save existing warning
        String savedWarning = currentWarning;
        setStatusMessage(service.calculateEarnings());
        // Restore the warning
        currentWarning = savedWarning;
        updateStatusArea();
    }

    @FXML
    public void onShowSummaryByMonthAndYear() {
        String summary = service.getSummaryByMonthAndYear();

        // Add warning for the current month if needed
        String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros());

        setStatusMessage(summary);
        if (warning != null) {
            setWarning(warning);
        } else {
            clearWarning();
        }
    }

    @FXML
    public void onExportExcel() {
        Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        choiceAlert.setTitle("Export Options");
        choiceAlert.setHeaderText("Choose export mode");
        choiceAlert.setContentText("Do you want to export all records, or only the ones currently filtered?\n\n"
                + "Deseja exportar todos os registros ou apenas os filtrados?");

        ButtonType exportFiltered = new ButtonType("Only Filtered / Apenas Filtrados");
        ButtonType exportAll = new ButtonType("Export All / Exportar Tudo");
        ButtonType cancel = new ButtonType("Cancel / Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        choiceAlert.getButtonTypes().setAll(exportFiltered, exportAll, cancel);

        Optional<ButtonType> result = choiceAlert.showAndWait();

        if (result.isEmpty() || result.get() == cancel) {
            setStatusMessage("❌ Export canceled.\n❌ Exportação cancelada.");
            return;
        }

        List<DisplayEntry> entriesToExport = new ArrayList<>();

        if (result.get() == exportAll) {
            for (RegistroTrabalho r : service.getRegistros()) {
                entriesToExport.add(new DisplayEntry(r));
            }

            // Clear bill cache to ensure fresh data
            service.clearBillCache();

            for (String year : yearToMonthsMap.keySet()) {
                for (String month : yearToMonthsMap.getOrDefault(year, Collections.emptyList())) {
                    for (Bill b : service.getBillsForMonth(year + "-" + month)) {
                        entriesToExport.add(new DisplayEntry(b));
                    }
                }
            }
        } else {
            entriesToExport.addAll(displayEntries);  // current filtered view
        }

        try {
            boolean isAllExport = (result.get() == exportAll);
            ExcelExporter.exportToExcel(entriesToExport, service, isAllExport);
            setStatusMessage("✔ Exported to 'documents/worklog/exports'.\n✔ Exportado para a pasta 'documents/worklog/exports'.");
        } catch (IOException e) {
            setStatusMessage("❌ Export error: " + e.getMessage() + "\n❌ Erro na exportação: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showStartupWarnings() {
        String warning = WarningUtils.generateStartupWarningBlock(service.getRegistros());
        if (warning != null) {
            setWarning(warning);

            // Show startup popup for current month
            javafx.application.Platform.runLater(() -> {
                WarningUtils.showStartupWarningIfNeeded(service.getRegistros());
            });
        }
    }
}