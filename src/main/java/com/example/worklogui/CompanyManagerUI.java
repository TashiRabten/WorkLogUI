
package com.example.worklogui;

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
        import java.time.format.DateTimeParseException;
        import java.util.*;





/**
 * Updated Company Manager UI with DisplayEntry (RegistroTrabalho or Bill).
 */



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

    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            service.initialize();

            // 🟡 Populate year-to-months map BEFORE setupFilters()
            yearToMonthsMap.clear();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            yearToMonthsMap.clear();
            for (RegistroTrabalho r : service.getRegistros()) {
                updateYearToMonthsMapFromEntry(r);
            }

            setupFilters();         // ✅ Relies on yearToMonthsMap now
            reloadCompanyList();
            onApplyFilter();        // Prepopulate table with filtered view
            setupRowDoubleClick();  // Double-click handler for rows

        } catch (Exception e) {
            statusArea.setText("Erro ao inicializar / Error initializing: " + e.getMessage());
            e.printStackTrace();
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

        String current = monthFilter.getValue();
        if (!months.contains(current)) {
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
        List<String> years = new ArrayList<>(service.getYears());
        Collections.sort(years);
        years.add(0, "All");
        yearFilter.setItems(FXCollections.observableArrayList(years));

        List<String> companies = new ArrayList<>(service.getCompanies());
        Collections.sort(companies);
        companies.add(0, "All"); // 🟢 Always keep "All" as first item
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
        if (year == null || month == null) {
            statusArea.setText("⚠️ Please select both year and month to edit bills.\n⚠️ Por favor, selecione ano e mês.");
            return;
        }
        new BillsEditorUI(year + "-" + month, service, (Stage) editBillsBtn.getScene().getWindow()).show();
    }

    @FXML
    public void onLogWork() {
        try {
            String rawDate = dateField.getText().trim();
            String empresa = jobTypeCombo.getValue();
            String rawValue = valueField.getText().trim();
            boolean dobro = doublePayCheckBox.isSelected();

            if (rawDate.isEmpty() || empresa == null || rawValue.isEmpty()) {
                statusArea.setText("All fields are required.\nTodos os campos são obrigatórios.");
                return;
            }

            LocalDate parsedDate = DateParser.parseDate(rawDate);
            double valor = Double.parseDouble(rawValue);
            RegistroTrabalho newEntry = service.logWork(parsedDate, empresa, valor, dobro);
            displayEntries.add(new DisplayEntry(newEntry));
            updateTable(new ArrayList<>(displayEntries));

            // ✅ Update internal map for filters
            updateYearToMonthsMapFromEntry(newEntry);

            // 🔁 Conditionally update filters to make new entry visible
            String newYear = String.valueOf(parsedDate.getYear());
            String newMonth = String.format("%02d", parsedDate.getMonthValue());

            boolean yearMatches = newYear.equals(yearFilter.getValue());
            boolean monthMatches = newMonth.equals(monthFilter.getValue()) || "All".equals(monthFilter.getValue());

            if (!yearMatches || !monthMatches) {
                yearFilter.setValue(newYear);
                updateMonthFilter(); // must update before setting month
                monthFilter.setValue(newMonth);
            }

            logTable.refresh();
            dateField.clear();
            valueField.clear();
            doublePayCheckBox.setSelected(false);
            statusArea.setText("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");

            onApplyFilter();  // shows entry with the correct filter
        } catch (Exception e) {
            statusArea.setText("❌ Error: " + e.getMessage());
        }
    }




    private void updateTable(List<DisplayEntry> entries) {
        entries.sort(Comparator.comparing(DisplayEntry::getDate));
        displayEntries.setAll(entries);
        logTable.setItems(displayEntries);
    }

    private void updateYearToMonthsMapFromEntry(RegistroTrabalho r) {
        try {
            LocalDate date = LocalDate.parse(r.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            String year = String.valueOf(date.getYear());
            String month = String.format("%02d", date.getMonthValue());

            List<String> months = yearToMonthsMap.computeIfAbsent(year, k -> new ArrayList<>());
            if (!months.contains(month)) {
                months.add(month);
                Collections.sort(months);  // Optional: keep months ordered
            }
        } catch (Exception ignored) {}
    }



    @FXML
    public void onApplyFilter() {
        String y = yearFilter.getValue();
        String m = monthFilter.getValue();
        String c = companyFilter.getValue();

        if ("All".equals(c)) c = null;
        if ("All".equals(m)) m = null;
        boolean allYears = "All".equals(y);

        List<DisplayEntry> combined = new ArrayList<>();
        double grossTotal = 0.0;
        double billTotal = 0.0;

        if (allYears) {
            for (String year : service.getYears()) {
                List<RegistroTrabalho> logs = service.applyFilters(year, null, c);
                for (RegistroTrabalho r : logs) {
                    combined.add(new DisplayEntry(r));
                    grossTotal += service.calculateEarnings(r);
                }
                for (String month : yearToMonthsMap.getOrDefault(year, Collections.emptyList())) {
                    for (Bill b : service.getBillsForMonth(year + "-" + month)) {
                        combined.add(new DisplayEntry(b));
                        billTotal += b.getAmount();
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
                    String ym = y + "-" + m;
                    for (Bill b : service.getBillsForMonth(ym)) {
                        combined.add(new DisplayEntry(b));
                        billTotal += b.getAmount();
                    }
                } else {
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
        double net = grossTotal - billTotal;

        netTotalLabel.setText(String.format("📈 Gross: $%.2f    💸 Bills: $%.2f    📉 Net Total: $%.2f", grossTotal, billTotal, net));
        statusArea.setText(String.format("✔ Filter applied. Showing %d entries.", combined.size()));
    }





    @FXML
    public void onClearFilter() {
        // Do not modify the filter ComboBoxes — preserve user's selection

        // Clear displayed results
        logTable.setItems(FXCollections.observableArrayList());
        statusArea.setText("✔ Display cleared. Filters unchanged.\n✔ Tela limpa. Filtros mantidos.");
        netTotalLabel.setText("📉 Net Total: —");
    }



    @FXML
    public void onShowTotal() {
        statusArea.setText(service.calculateTimeTotal());
    }

    @FXML
    public void onShowEarnings() {
        statusArea.setText(service.calculateEarnings());
    }

    @FXML
    public void onShowSummaryByMonthAndYear() {
        statusArea.setText(service.getSummaryByMonthAndYear());
    }
    @FXML
    public void onExportCsv() {
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
            statusArea.setText("❌ Export canceled.");
            return;
        }

        List<DisplayEntry> entriesToExport = new ArrayList<>();

        if (result.get() == exportAll) {
            for (RegistroTrabalho r : service.getRegistros()) {
                entriesToExport.add(new DisplayEntry(r));
            }
            for (String year : service.getYears()) {
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
            CsvExporter.exportToCsv(entriesToExport, service);
            statusArea.setText("✔ Exported to 'documents/worklog/exports'.\n✔ Exportado para a pasta 'documents/worklog/exports'.");
        } catch (IOException e) {
            statusArea.setText("❌ Export error: " + e.getMessage());
        }
    }



    @FXML
    public void onOpenLogEditor() {
        LogEditorUI editor = new LogEditorUI();
        editor.setOnClose(() -> {
            try {
                service.reloadRegistros();
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
            statusArea.setText("⚠️ No record selected.\n⚠️ Nenhum registro selecionado.");
            return;
        }
        onOpenLogEditor();
    }

    @FXML
    public void onDeleteLogEntry() {
        DisplayEntry selected = logTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBill()) {
            statusArea.setText("⚠️ No record selected.\n⚠️ Nenhum registro selecionado.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this entry?\nTem certeza que deseja excluir este registro?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    service.deleteRegistro(selected.getRegistro());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                onApplyFilter();
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
