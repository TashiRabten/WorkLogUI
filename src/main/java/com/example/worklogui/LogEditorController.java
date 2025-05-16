package com.example.worklogui;

import javafx.css.converter.StringConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import javafx.scene.control.DatePicker;
import javafx.application.Platform;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class LogEditorController {

    @FXML
    private TableView<RegistroTrabalho> logTable;

    @FXML
    private TableColumn<RegistroTrabalho, String> dateColumn;

    @FXML
    private TableColumn<RegistroTrabalho, String> companyColumn;

    @FXML
    private Button editLogBtn;

    @FXML
    private Button deleteLogBtn;

    @FXML
    private Button clearAllLogsBtn;

    @FXML
    private Button closeBtn;

    @FXML
    private VBox rootContainer;

    private List<RegistroTrabalho> registros = new ArrayList<>();
    private Runnable onSaveCallback;

    // NEW: Filter tracking fields
    private String currentFilterYear;
    private String currentFilterMonth;
    private String currentFilterCompany;
    private BiConsumer<String, String> onFilterCallback;

    public void setRegistros(List<RegistroTrabalho> registros) {
        this.registros = registros;
        if (logTable != null) {
            refreshLogTable();
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    /**
     * NEW: Method to store filter values
     */
    public void setFilterValues(String year, String month, String company) {
        this.currentFilterYear = year;
        this.currentFilterMonth = month;
        this.currentFilterCompany = company;
        System.out.println("Filter values set: year=" + year + ", month=" + month + ", company=" + company);
    }

    /**
     * NEW: Method to set filter callback
     */
    public void setOnFilterCallback(BiConsumer<String, String> callback) {
        this.onFilterCallback = callback;
    }

    @FXML
    public void initialize() {
        logTable.getColumns().clear();
        setupLogTable();
    }

    private void setupLogTable() {
        try {
            // Check if we already have the dateColumn from FXML
            if (dateColumn != null) {
                dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                        cellData.getValue().getData()));
            } else {
                // Create date column if it doesn't exist
                TableColumn<RegistroTrabalho, String> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(cellData -> {
                    try {
                        LocalDate parsed = LocalDate.parse(cellData.getValue().getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        return new SimpleStringProperty(parsed.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                    } catch (Exception e) {
                        return new SimpleStringProperty(cellData.getValue().getData());
                    }
                });

                dateCol.setPrefWidth(100);
                logTable.getColumns().add(dateCol);
            }

            // Check if we already have the companyColumn from FXML
            if (companyColumn != null) {
                companyColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                        cellData.getValue().getEmpresa()));
            } else {
                // Create company column if it doesn't exist
                TableColumn<RegistroTrabalho, String> companyCol = new TableColumn<>("Company");
                companyCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                        cellData.getValue().getEmpresa()));
                companyCol.setPrefWidth(120);
                logTable.getColumns().add(companyCol);
            }

            // Create hours column
            TableColumn<RegistroTrabalho, String> hoursCol = new TableColumn<>("Hours");
            hoursCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                    String.format("%.2f", cellData.getValue().getHoras())));
            hoursCol.setPrefWidth(70);
            logTable.getColumns().add(hoursCol);

            // Create minutes column
            TableColumn<RegistroTrabalho, String> minutesCol = new TableColumn<>("Minutes");
            minutesCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                    String.format("%.0f", cellData.getValue().getMinutos())));
            minutesCol.setPrefWidth(70);
            logTable.getColumns().add(minutesCol);

            // Create double pay column
            TableColumn<RegistroTrabalho, String> doublePayCol = new TableColumn<>("Double Pay");
            doublePayCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().isPagamentoDobrado() ? "Yes" : "No"));
            doublePayCol.setPrefWidth(80);
            logTable.getColumns().add(doublePayCol);

            // Create earnings column
            TableColumn<RegistroTrabalho, String> earningsCol = new TableColumn<>("Earnings");
            earningsCol.setCellValueFactory(cellData -> {
                RegistroTrabalho r = cellData.getValue();
                String tipo = r.getTipoUsado() != null ? r.getTipoUsado() : "hour";
                double taxa = r.getTaxaUsada();
                double earnings;

                if (tipo.equalsIgnoreCase("minuto")) {
                    earnings = r.getMinutos() * taxa;
                } else {
                    earnings = (r.getHoras() * taxa) + (r.getMinutos() * (taxa / 60.0));
                }

                if (r.isPagamentoDobrado()) {
                    earnings *= 2;
                }

                return new SimpleStringProperty(String.format("$%.2f", earnings));
            });

            earningsCol.setPrefWidth(80);
            logTable.getColumns().add(earningsCol);

            // Add row double-click handler for editing
            logTable.setRowFactory(tv -> {
                TableRow<RegistroTrabalho> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        editLogEntry(row.getItem());
                    }
                });
                return row;
            });

            System.out.println("Added " + logTable.getColumns().size() + " columns to table");

        } catch (Exception e) {
            System.out.println("Error setting up table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshLogTable() {
        logTable.getItems().clear();
        logTable.getItems().addAll(registros);
    }

    @FXML
    public void onEditLog() {
        RegistroTrabalho selected = logTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editLogEntry(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection / Nenhuma Seleção",
                    "Please select a log entry to edit.\nPor favor, selecione um registro para editar.");
        }
    }

    /**
     * UPDATED: onDeleteLog with immediate save
     */
    @FXML
    public void onDeleteLog() {
        RegistroTrabalho selected = logTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Selected log for deletion: " + selected.getEmpresa() + " on " + selected.getData());

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete this log entry?\nTem certeza de que deseja excluir este registro?",
                    ButtonType.YES, ButtonType.NO);
            confirmation.setHeaderText("Confirm Deletion / Confirmar Exclusão");

            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    // Remove from the local list
                    boolean removed = registros.remove(selected);
                    System.out.println("Removed from registros list: " + removed);

                    // CRITICAL: Immediately save changes to file
                    try {
                        System.out.println("Saving changes to file");
                        FileLoader.salvarRegistros(AppConstants.WORKLOG_PATH, registros);

                        // Update the table display
                        logTable.getItems().remove(selected);
                        logTable.refresh();
                        System.out.println("Table refreshed after deletion");

                        // Notify via callback
                        if (onSaveCallback != null) {
                            System.out.println("Running onSaveCallback");
                            onSaveCallback.run();
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Success / Sucesso",
                                "✔ Log deleted.\n✔ Registro excluído.");
                    } catch (Exception e) {
                        System.err.println("Error saving changes: " + e.getMessage());
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error / Erro",
                                "Failed to save changes:\nFalha ao salvar alterações:\n" + e.getMessage());
                    }
                }
            });
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection / Nenhuma Seleção",
                    "Please select a log entry to delete.\nPor favor, selecione um registro para excluir.");
        }
    }
    @FXML
    public void onClearAllLogs() {
        // Debug output
        System.out.println("===== DEBUG INFO =====");
        System.out.println("Current filter year: " + currentFilterYear);
        System.out.println("Current filter month: " + currentFilterMonth);
        System.out.println("Current filter company: " + currentFilterCompany);
        System.out.println("Total registros: " + registros.size());

        // Check each log against filter
        int matchingLogs = 0;
        for (RegistroTrabalho log : registros) {
            boolean matches = matchesFilter(log);
            if (matches) {
                matchingLogs++;
                System.out.println("Log matches filter: " + log.getData() + " - " + log.getEmpresa());
            }
        }
        System.out.println("Logs matching filter: " + matchingLogs);
        System.out.println("=====================");

        // First check if we have any active filters
        final boolean hasFilter =
                (currentFilterYear != null && !currentFilterYear.equals("All")) ||
                        (currentFilterMonth != null && !currentFilterMonth.equals("All")) ||
                        (currentFilterCompany != null && !currentFilterCompany.equals("All"));

        String confirmMessage = hasFilter ?
                "Are you sure you want to delete all FILTERED log entries?\nTem certeza de que deseja excluir TODOS os registros FILTRADOS?" :
                "Are you sure you want to delete ALL log entries?\nTem certeza de que deseja excluir TODOS os registros?";

        String headerText = hasFilter ?
                "Clear Filtered Logs / Limpar Registros Filtrados" :
                "Confirm Delete All / Confirmar Exclusão Total";

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                confirmMessage, ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText(headerText);

        final AtomicInteger removedCount = new AtomicInteger(0);

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                if (hasFilter) {
                    System.out.println("Clearing with filters: year=" + currentFilterYear +
                            ", month=" + currentFilterMonth +
                            ", company=" + currentFilterCompany);

                    // KEY FIX: Load ALL logs from the file first
                    try {
                        List<RegistroTrabalho> allLogs = FileLoader.carregarRegistros(AppConstants.WORKLOG_PATH);
                        System.out.println("Loaded " + allLogs.size() + " total logs from file");

                        // Create a new list to hold logs we want to keep
                        List<RegistroTrabalho> logsToKeep = new ArrayList<>();

                        // Go through all logs and keep only those that don't match our filter
                        for (RegistroTrabalho log : allLogs) {
                            if (!matchesFilter(log)) {
                                // If it doesn't match the filter, keep it
                                logsToKeep.add(log);
                            } else {
                                // If it matches the filter, count it for removal
                                removedCount.incrementAndGet();
                                System.out.println("Removing log: " + log.getEmpresa() + " on " + log.getData());
                            }
                        }

                        System.out.println("Keeping " + logsToKeep.size() + " logs, removing " + removedCount.get());

                        // Save the filtered list back to file
                        FileLoader.salvarRegistros(AppConstants.WORKLOG_PATH, logsToKeep);

                        // Update our local list for display
                        registros.clear();
                        // Only add logs that match our filter for display
                        for (RegistroTrabalho log : logsToKeep) {
                            if (matchesFilter(log)) {
                                registros.add(log);
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("Error loading or saving all logs: " + e.getMessage());
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error / Erro",
                                "Failed to process logs: " + e.getMessage());
                        return;
                    }
                } else {
                    // If no filters, clear everything
                    removedCount.set(registros.size());
                    System.out.println("Clearing all " + removedCount.get() + " logs");
                    registros.clear();

                    // Save empty list back to file
                    try {
                        FileLoader.salvarRegistros(AppConstants.WORKLOG_PATH, registros);
                    } catch (Exception e) {
                        System.err.println("Error saving empty log list: " + e.getMessage());
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error / Erro",
                                "Failed to save changes: " + e.getMessage());
                        return;
                    }
                }

                // Refresh the table
                refreshLogTable();

                // Notify via callback
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }

                // Create a final copy of the count for the success message
                final int finalRemovedCount = removedCount.get();

                // Show success message
                String successMessage = hasFilter ?
                        "✔ All filtered logs cleared (" + finalRemovedCount + " entries).\n✔ Todos os registros filtrados foram excluídos (" + finalRemovedCount + " entradas)." :
                        "✔ All logs cleared.\n✔ Todos os registros foram excluídos.";

                showAlert(Alert.AlertType.INFORMATION, "Success / Sucesso", successMessage);
            }
        });
    }

    /**
     * Helper method to check if a log matches the current filter
     */
    private boolean matchesFilter(RegistroTrabalho log) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate date = LocalDate.parse(log.getData(), formatter);

            // Check year filter
            if (currentFilterYear != null && !currentFilterYear.equals("All")) {
                String logYear = String.valueOf(date.getYear());
                if (!logYear.equals(currentFilterYear)) {
                    return false;
                }
            }

            // Check month filter
            if (currentFilterMonth != null && !currentFilterMonth.equals("All")) {
                String logMonth = String.format("%02d", date.getMonthValue());
                if (!logMonth.equals(currentFilterMonth)) {
                    return false;
                }
            }

            // Check company filter
            if (currentFilterCompany != null && !currentFilterCompany.equals("All")) {
                if (!log.getEmpresa().equals(currentFilterCompany)) {
                    return false;
                }
            }

            // If we got here, the entry matches all active filters
            return true;
        } catch (Exception e) {
            System.err.println("Error checking if log matches filter: " + e.getMessage());
            // If there's an error, assume it doesn't match
            return false;
        }
    }


    @FXML
    public void onClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    private void editLogEntry(RegistroTrabalho entry) {
        // Create a dialog for editing
        Dialog<RegistroTrabalho> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        dialog.setTitle("Edit Log Entry");
        dialog.setHeaderText("Edit Work Log Entry");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Date picker instead of text field
        DatePicker datePicker = new DatePicker();
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return formatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, formatter);
                } else {
                    return null;
                }
            }
        });

        // Store original date for comparison later
        LocalDate originalDate = null;
        try {
            originalDate = LocalDate.parse(entry.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            datePicker.setValue(originalDate);
        } catch (Exception e) {
            // If parsing fails, set to today
            datePicker.setValue(LocalDate.now());
        }

        // Company dropdown
        ComboBox<String> companyCombo = new ComboBox<>();
        companyCombo.getItems().addAll(CompanyRateService.getInstance().getRates().keySet());
        companyCombo.setValue(entry.getEmpresa());

        // Hours field
        TextField hoursField = new TextField(String.format("%.2f", entry.getHoras()));

        // Minutes field
        TextField minutesField = new TextField(String.format("%.0f", entry.getMinutos()));

        // Double pay checkbox
        CheckBox doublePayCheck = new CheckBox("Double Pay");
        doublePayCheck.setSelected(entry.isPagamentoDobrado());

        // Add fields to grid
        grid.add(new Label("Date:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Company:"), 0, 1);
        grid.add(companyCombo, 1, 1);
        grid.add(new Label("Hours:"), 0, 2);
        grid.add(hoursField, 1, 2);
        grid.add(new Label("Minutes:"), 0, 3);
        grid.add(minutesField, 1, 3);
        grid.add(doublePayCheck, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the date picker by default
        Platform.runLater(() -> datePicker.requestFocus());

        // Final reference to original date for use in lambda
        final LocalDate finalOriginalDate = originalDate;

        // Convert the result to a RegistroTrabalho object when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Get date from DatePicker
                    LocalDate selectedDate = datePicker.getValue();
                    if (selectedDate == null) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Date",
                                "Please select a valid date.");
                        return null;
                    }

                    // Format date to MM/dd/yyyy
                    String date = selectedDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

                    // Validate hours
                    double hours;
                    try {
                        hours = Double.parseDouble(hoursField.getText().trim());
                        if (hours < 0) throw new NumberFormatException("Hours cannot be negative");
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Hours",
                                "Please enter a valid positive number for hours.");
                        return null;
                    }

                    // Validate minutes
                    double minutes;
                    try {
                        minutes = Double.parseDouble(minutesField.getText().trim());
                        if (minutes < 0) throw new NumberFormatException("Minutes cannot be negative");
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Minutes",
                                "Please enter a valid positive number for minutes.");
                        return null;
                    }

                    RegistroTrabalho updated = new RegistroTrabalho();
                    updated.setData(date);
                    updated.setEmpresa(companyCombo.getValue());
                    updated.setHoras(hours);
                    updated.setMinutos(minutes);
                    updated.setPagamentoDobrado(doublePayCheck.isSelected());
                    updated.setTaxaUsada(entry.getTaxaUsada());
                    updated.setTipoUsado(entry.getTipoUsado());

                    return updated;
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show the dialog and process the result
        Optional<RegistroTrabalho> result = dialog.showAndWait();

        result.ifPresent(updatedEntry -> {
            // Find the original entry index and replace it
            int index = registros.indexOf(entry);
            if (index >= 0) {
                registros.set(index, updatedEntry);

                // Save immediately
                try {
                    FileLoader.salvarRegistros(AppConstants.WORKLOG_PATH, registros);

                    // Get the new date
                    LocalDate newDate = LocalDate.parse(updatedEntry.getData(),
                            DateTimeFormatter.ofPattern("MM/dd/yyyy"));

                    // Check if the date changed - FIXED: Corrected the boolean expression syntax
                    boolean dateChanged = finalOriginalDate != null &&
                            (newDate.getYear() != finalOriginalDate.getYear() ||
                                    newDate.getMonthValue() != finalOriginalDate.getMonthValue());

                    System.out.println("Date changed: " + dateChanged);
                    System.out.println("Original date: " + finalOriginalDate);
                    System.out.println("New date: " + newDate);

                    // Update the table
                    refreshLogTable();

                    // If the date changed, use the filter callback
                    if (dateChanged && onFilterCallback != null) {
                        System.out.println("Using onFilterCallback to update filter to new date");
                        onFilterCallback.accept(
                                String.valueOf(newDate.getYear()),
                                String.format("%02d", newDate.getMonthValue())
                        );
                    }
                    // Otherwise use the standard callback
                    else if (onSaveCallback != null) {
                        System.out.println("Using onSaveCallback for regular update");
                        onSaveCallback.run();
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Success / Sucesso",
                            "✔ Log entry updated.\n✔ Registro atualizado.");
                } catch (Exception e) {
                    System.err.println("Error saving changes: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error / Erro",
                            "Failed to save changes:\nFalha ao salvar alterações:\n" + e.getMessage());
                }
            }
        });
    }

    private void saveAndRefreshLogs(String successMessage) {
        try {
            FileLoader.salvarRegistros(AppConstants.WORKLOG_PATH, registros);
            refreshLogTable();
            if (onSaveCallback != null) onSaveCallback.run();
            showAlert(Alert.AlertType.INFORMATION, "Success / Sucesso", successMessage);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error / Erro",
                    "Failed to save changes:\nFalha ao salvar alterações:\n" + e.getMessage());
        }
    }
    @FXML
    public void onSave() {
        try {
            // Save to file
            FileLoader.salvarRegistros(AppConstants.WORKLOG_PATH, registros);

            // Refresh the table
            refreshLogTable();

            // Call the callback to refresh filters in the main UI
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Success / Sucesso",
                    "✓ Changes saved successfully.\n✓ Alterações salvas com sucesso.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error / Erro",
                    "Failed to save changes:\nFalha ao salvar alterações:\n" + e.getMessage());
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