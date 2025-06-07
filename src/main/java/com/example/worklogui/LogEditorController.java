package com.example.worklogui;

import com.example.worklogui.exceptions.WorkLogNotFoundException;
import com.example.worklogui.exceptions.WorkLogServiceException;
import com.example.worklogui.services.WorkLogBusinessService;
import com.example.worklogui.utils.DateUtils;
import com.example.worklogui.utils.ErrorHandler;
import com.example.worklogui.utils.FilterHelper;
import com.example.worklogui.utils.ProgressDialog;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private CompanyManagerService service;
    private WorkLogBusinessService businessService;

    // Filter tracking fields
    private String currentFilterYear;
    private String currentFilterMonth;
    private String currentFilterCompany;
    private BiConsumer<String, String> onFilterCallback;

    public void setService(CompanyManagerService service) {
        this.service = service;
        this.businessService = new WorkLogBusinessService(service.getWorkLogFileManager());
    }

    public void setRegistros(List<RegistroTrabalho> registros) {
        this.registros = new ArrayList<>(registros); // Defensive copy
        if (logTable != null) {
            refreshLogTable();
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setFilterValues(String year, String month, String company) {
        this.currentFilterYear = year;
        this.currentFilterMonth = month;
        this.currentFilterCompany = company;
        System.out.println("Filter values set: year=" + year + ", month=" + month + ", company=" + company);
    }

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
            // Date column
            if (dateColumn != null) {
                dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                        cellData.getValue().getData()));
            } else {
                TableColumn<RegistroTrabalho, String> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(cellData -> {
                    try {
                        LocalDate parsed = DateUtils.parseDisplayDate(cellData.getValue().getData());
                        return new SimpleStringProperty(DateUtils.formatDisplayDate(parsed));
                    } catch (Exception e) {
                        return new SimpleStringProperty(cellData.getValue().getData());
                    }
                });
                dateCol.setPrefWidth(100);
                logTable.getColumns().add(dateCol);
            }

            // Company column
            if (companyColumn != null) {
                companyColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                        cellData.getValue().getEmpresa()));
            } else {
                TableColumn<RegistroTrabalho, String> companyCol = new TableColumn<>("Company");
                companyCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                        cellData.getValue().getEmpresa()));
                companyCol.setPrefWidth(120);
                logTable.getColumns().add(companyCol);
            }

            // Hours column
            TableColumn<RegistroTrabalho, String> hoursCol = new TableColumn<>("Hours");
            hoursCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                    String.format("%.2f", cellData.getValue().getHoras())));
            hoursCol.setPrefWidth(70);
            logTable.getColumns().add(hoursCol);

            // Minutes column
            TableColumn<RegistroTrabalho, String> minutesCol = new TableColumn<>("Minutes");
            minutesCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                    String.format("%.0f", cellData.getValue().getMinutos())));
            minutesCol.setPrefWidth(70);
            logTable.getColumns().add(minutesCol);

            // Double pay column
            TableColumn<RegistroTrabalho, String> doublePayCol = new TableColumn<>("Double Pay");
            doublePayCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().isPagamentoDobrado() ? "Yes" : "No"));
            doublePayCol.setPrefWidth(80);
            logTable.getColumns().add(doublePayCol);

            // Earnings column
            TableColumn<RegistroTrabalho, String> earningsCol = new TableColumn<>("Earnings");
            earningsCol.setCellValueFactory(cellData -> {
                RegistroTrabalho r = cellData.getValue();
                if (businessService != null) {
                    double earnings = businessService.calculateEarnings(r);
                    return new SimpleStringProperty(String.format("$%.2f", earnings));
                } else {
                    return new SimpleStringProperty("N/A");
                }
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
                    performDeleteInBackground(selected);
                }
            });
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection / Nenhuma Seleção",
                    "Please select a log entry to delete.\nPor favor, selecione um registro para excluir.");
        }
    }

    @FXML
    public void onClearAllLogs() {
        logClearDebugInfo();
        final boolean hasFilter = FilterHelper.hasActiveFilters(currentFilterYear, currentFilterMonth, currentFilterCompany);

        if (confirmClearOperation(hasFilter)) {
            performClearLogsInBackground(hasFilter);
        }
    }

    /**
     * Performs clear logs operation in background thread to avoid UI blocking
     */
    private void performClearLogsInBackground(boolean hasFilter) {
        Task<Integer> clearTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                updateMessage("Preparing to clear logs...");
                updateProgress(0, 100);

                AtomicInteger removedCount = new AtomicInteger(0);

                if (hasFilter) {
                    updateMessage("Clearing filtered logs...");
                    updateProgress(25, 100);
                    clearFilteredLogs(removedCount);
                } else {
                    updateMessage("Clearing all logs...");
                    updateProgress(25, 100);
                    clearAllLogs(removedCount);
                }

                updateProgress(100, 100);
                updateMessage("Clear operation completed");
                return removedCount.get();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    handleClearSuccess(hasFilter, getValue());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    handleClearError(exception);
                });
            }
        };

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(clearTask);
        progressDialog.setTitle("Clearing Logs");
        progressDialog.setHeaderText("Please wait while logs are being cleared...");

        // Run task in background
        Thread clearThread = new Thread(clearTask);
        clearThread.setDaemon(true);
        clearThread.start();

        progressDialog.showAndWait();
    }

    /**
     * Performs delete operation in background thread
     */
    private void performDeleteInBackground(RegistroTrabalho selected) {
        Task<Void> deleteTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Deleting log entry...");
                updateProgress(50, 100);

                if (businessService != null) {
                    businessService.deleteWorkLog(selected);
                }

                updateProgress(100, 100);
                updateMessage("Delete completed");
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Remove from local list and UI
                    registros.remove(selected);
                    logTable.getItems().remove(selected);
                    logTable.refresh();

                    if (onSaveCallback != null) {
                        onSaveCallback.run();
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Success / Sucesso",
                            "✔ Log deleted.\n✔ Registro excluído.");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable throwable = getException();
                    Exception exception = (throwable instanceof Exception) ?
                            (Exception) throwable :
                            new Exception("Task failed", throwable);
                    ErrorHandler.handleUnexpectedError("deleting log entry", exception);
                    showAlert(Alert.AlertType.ERROR, "Error / Erro",
                            "Failed to delete log entry:\nFalha ao excluir registro:\n" + throwable.getMessage());
                });
            }
        };

        Thread deleteThread = new Thread(deleteTask);
        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    private void logClearDebugInfo() {
        System.out.println("===== DEBUG INFO =====");
        System.out.println("Current filter year: " + currentFilterYear);
        System.out.println("Current filter month: " + currentFilterMonth);
        System.out.println("Current filter company: " + currentFilterCompany);
        System.out.println("Total registros: " + registros.size());

        int matchingLogs = 0;
        for (RegistroTrabalho log : registros) {
            boolean matches = FilterHelper.matchesFilters(log, currentFilterYear, currentFilterMonth, currentFilterCompany);
            if (matches) {
                matchingLogs++;
                System.out.println("Log matches filter: " + log.getData() + " - " + log.getEmpresa());
            }
        }
        System.out.println("Logs matching filter: " + matchingLogs);
        System.out.println("=====================");
    }

    private boolean confirmClearOperation(boolean hasFilter) {
        String confirmMessage = hasFilter ?
                "Are you sure you want to delete all FILTERED log entries?\nTem certeza de que deseja excluir TODOS os registros FILTRADOS?" :
                "Are you sure you want to delete ALL log entries?\nTem certeza de que deseja excluir TODOS os registros?";

        String headerText = hasFilter ?
                "Clear Filtered Logs / Limpar Registros Filtrados" :
                "Confirm Delete All / Confirmar Exclusão Total";

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                confirmMessage, ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText(headerText);

        return confirmation.showAndWait()
                .map(response -> response == ButtonType.YES)
                .orElse(false);
    }

    private void clearFilteredLogs(AtomicInteger removedCount) throws Exception {
        System.out.println("Clearing with filters: year=" + currentFilterYear +
                ", month=" + currentFilterMonth +
                ", company=" + currentFilterCompany);

        List<RegistroTrabalho> allLogs = service != null ? service.getRegistros() : new ArrayList<>();
        System.out.println("Loaded " + allLogs.size() + " total logs from service");

        List<RegistroTrabalho> logsToKeep = new ArrayList<>();
        for (RegistroTrabalho log : allLogs) {
            if (!FilterHelper.matchesFilters(log, currentFilterYear, currentFilterMonth, currentFilterCompany)) {
                logsToKeep.add(log);
            } else {
                removedCount.incrementAndGet();
                System.out.println("Removing log: " + log.getEmpresa() + " on " + log.getData());
            }
        }

        System.out.println("Keeping " + logsToKeep.size() + " logs, removing " + removedCount.get());

        if (service != null) {
            saveUpdatedLogsToService(logsToKeep);
        }

        updateLocalDisplayAfterFiltering(logsToKeep);
    }

    private void clearAllLogs(AtomicInteger removedCount) throws WorkLogNotFoundException, WorkLogServiceException {
        removedCount.set(registros.size());
        System.out.println("Clearing all " + removedCount.get() + " logs");

        if (service != null) {
            for (RegistroTrabalho log : new ArrayList<>(registros)) {
                service.deleteRegistro(log);
            }
        }
        registros.clear();
    }

    private void updateLocalDisplayAfterFiltering(List<RegistroTrabalho> logsToKeep) {
        registros.clear();
        for (RegistroTrabalho log : logsToKeep) {
            if (FilterHelper.matchesFilters(log, currentFilterYear, currentFilterMonth, currentFilterCompany)) {
                registros.add(log);
            }
        }
    }

    private void handleClearSuccess(boolean hasFilter, int removedCount) {
        refreshLogTable();

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        String successMessage = hasFilter ?
                "✔ All filtered logs cleared (" + removedCount + " entries).\n✔ Todos os registros filtrados foram excluídos (" + removedCount + " entradas)." :
                "✔ All logs cleared.\n✔ Todos os registros foram excluídos.";

        showAlert(Alert.AlertType.INFORMATION, "Success / Sucesso", successMessage);
    }

    private void handleClearError(Throwable throwable) {
        Exception exception = (throwable instanceof Exception) ?
                (Exception) throwable :
                new Exception("Clear operation failed", throwable);
        ErrorHandler.handleUnexpectedError("clearing logs", exception);
        showAlert(Alert.AlertType.ERROR, "Error / Erro",
                "Failed to clear logs:\nFalha ao limpar registros:\n" + throwable.getMessage());
    }

    private void saveUpdatedLogsToService(List<RegistroTrabalho> allLogs) throws Exception {
        // Group logs by month
        var monthlyLogs = FilterHelper.groupByYearMonth(allLogs);

        // Get current monthly files and clear them first
        List<String> existingKeys = service.getWorkLogFileManager().getAvailableYearMonthKeys();
        for (String key : existingKeys) {
            service.getWorkLogFileManager().saveWorkLogs(key, new ArrayList<>());
        }

        // Save updated logs by month
        for (var entry : monthlyLogs.entrySet()) {
            String yearMonth = entry.getKey();
            List<RegistroTrabalho> monthLogs = entry.getValue();
            service.getWorkLogFileManager().saveWorkLogs(yearMonth, monthLogs);
        }
    }

    @FXML
    public void onClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    private void editLogEntry(RegistroTrabalho entry) {
        Dialog<RegistroTrabalho> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        dialog.setTitle("Edit Log Entry");
        dialog.setHeaderText("Edit Work Log Entry");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Date picker
        DatePicker datePicker = new DatePicker();
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
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

        // Store original date for comparison
        LocalDate originalDate = null;
        try {
            originalDate = DateUtils.parseDisplayDate(entry.getData());
            datePicker.setValue(originalDate);
        } catch (Exception e) {
            datePicker.setValue(LocalDate.now());
        }

        // Company dropdown
        ComboBox<String> companyCombo = new ComboBox<>();
        companyCombo.getItems().addAll(CompanyRateService.getInstance().getRates().keySet());
        companyCombo.setValue(entry.getEmpresa());

        // Hours and minutes fields
        TextField hoursField = new TextField(String.format("%.2f", entry.getHoras()));
        TextField minutesField = new TextField(String.format("%.0f", entry.getMinutos()));

        // Double pay checkbox
        CheckBox doublePayCheck = new CheckBox("Double Pay");
        doublePayCheck.setSelected(entry.isPagamentoDobrado());

        // Add to grid
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
        Platform.runLater(() -> datePicker.requestFocus());

        final LocalDate finalOriginalDate = originalDate;

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    LocalDate selectedDate = datePicker.getValue();
                    if (selectedDate == null) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Date",
                                "Please select a valid date.");
                        return null;
                    }

                    String date = DateUtils.formatDisplayDate(selectedDate);
                    double hours = Double.parseDouble(hoursField.getText().trim());
                    double minutes = Double.parseDouble(minutesField.getText().trim());

                    if (hours < 0 || minutes < 0) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input",
                                "Hours and minutes must be positive.");
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

        Optional<RegistroTrabalho> result = dialog.showAndWait();

        result.ifPresent(updatedEntry -> {
            try {
                if (businessService != null) {
                    businessService.updateWorkLog(entry, updatedEntry);
                }

                // Update local list
                int index = registros.indexOf(entry);
                if (index >= 0) {
                    registros.set(index, updatedEntry);
                }

                // Get the new date
                LocalDate newDate = DateUtils.parseDisplayDate(updatedEntry.getData());

                // Check if the date changed
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
                ErrorHandler.handleUnexpectedError("updating log entry", e);
                showAlert(Alert.AlertType.ERROR, "Error / Erro",
                        "Failed to save changes:\nFalha ao salvar alterações:\n" + e.getMessage());
            }
        });
    }

    @FXML
    public void onSave() {
        try {
            // The service handles saving automatically, so just notify callback
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

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

    // NEW: Getter method for service access
    public CompanyManagerService getService() {
        return service;
    }
}