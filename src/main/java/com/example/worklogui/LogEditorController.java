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

    public void setRegistros(List<RegistroTrabalho> registros) {
        this.registros = registros;
        if (logTable != null) {
            refreshLogTable();
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
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

    @FXML
    public void onDeleteLog() {
        RegistroTrabalho selected = logTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete this log entry?\nTem certeza de que deseja excluir este registro?",
                    ButtonType.YES, ButtonType.NO);
            confirmation.setHeaderText("Confirm Deletion / Confirmar Exclusão");

            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    registros.remove(selected);
                    saveAndRefreshLogs("✔ Log deleted.\n✔ Registro excluído.");
                }
            });
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection / Nenhuma Seleção",
                    "Please select a log entry to delete.\nPor favor, selecione um registro para excluir.");
        }
    }

    @FXML
    public void onClearAllLogs() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete ALL log entries?\nTem certeza de que deseja excluir TODOS os registros?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Confirm Delete All / Confirmar Exclusão Total");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                registros.clear();
                saveAndRefreshLogs("✔ All logs cleared.\n✔ Todos os registros foram excluídos.");
            }
        });
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

        // Set the current date value
        try {
            LocalDate currentDate = LocalDate.parse(entry.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            datePicker.setValue(currentDate);
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
                saveAndRefreshLogs("Log entry updated successfully.");
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
