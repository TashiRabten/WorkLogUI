package com.example.worklogui;

import com.example.worklogui.exceptions.CompanyOperationException;
import com.example.worklogui.utils.DateUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CompanyEditorUI {

    private final TableView<Map.Entry<String, RateInfo>> table = new TableView<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, RateInfo> companyRates = new LinkedHashMap<>();
    private Runnable onCloseCallback;


    public void show(Stage parentStage) {
        loadRates();
        setupTableColumns();
        refreshTable();
        
        HBox buttons = createButtonPanel();
        VBox layout = new VBox(10, table, buttons);
        layout.setPadding(new Insets(15));
        
        Stage stage = createStage(parentStage, layout);
        stage.show();
        stage.setOnHiding(e -> {
            if (onCloseCallback != null) onCloseCallback.run();
        });
    }

    private void setupTableColumns() {
        TableColumn<Map.Entry<String, RateInfo>, String> nameCol = new TableColumn<>("Company");
        nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getKey()));
        nameCol.setPrefWidth(180);

        TableColumn<Map.Entry<String, RateInfo>, String> rateCol = new TableColumn<>("Rate");
        rateCol.setCellValueFactory(param -> {
            String displayType = param.getValue().getValue().getTipo().equals("hora") ? "hour" : "minute";
            return new javafx.beans.property.SimpleStringProperty(
                    String.format(Locale.US, "$ %.2f (%s)",
                            param.getValue().getValue().getValor(),
                            displayType)
            );
        });
        rateCol.setPrefWidth(150);

        table.getColumns().addAll(nameCol, rateCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private HBox createButtonPanel() {
        Button addBtn = new Button("Add");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button saveBtn = new Button("Save");
        Button closeBtn = new Button("Close");

        setupButtonActions(addBtn, editBtn, deleteBtn, saveBtn, closeBtn);

        HBox buttons = new HBox(10, addBtn, editBtn, deleteBtn, saveBtn, closeBtn);
        buttons.setPadding(new Insets(10));
        return buttons;
    }

    private void setupButtonActions(Button addBtn, Button editBtn, Button deleteBtn, Button saveBtn, Button closeBtn) {
        addBtn.setOnAction(e -> openEditor(null));
        editBtn.setOnAction(e -> handleEditAction());
        deleteBtn.setOnAction(e -> handleDeleteAction());
        saveBtn.setOnAction(e -> {
            saveRates();
            refreshTable();
        });
        closeBtn.setOnAction(e -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });
    }

    private void handleEditAction() {
        Map.Entry<String, RateInfo> selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openEditor(selected.getKey());
        }
    }

    private void handleDeleteAction() {
        Map.Entry<String, RateInfo> selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (confirmDeletion()) {
                companyRates.remove(selected.getKey());
                refreshTable();
                saveRates();
            }
        } else {
            showNoSelectionWarning();
        }
    }

    private boolean confirmDeletion() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Company / Excluir Empresa");
        confirm.setHeaderText("Are you sure you want to delete this company?\nTem certeza de que deseja excluir esta empresa?");
        confirm.setContentText("This action cannot be undone.\nEssa ação não pode ser desfeita.");

        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showNoSelectionWarning() {
        Alert warn = new Alert(Alert.AlertType.WARNING,
                "Please select a company to delete.\nPor favor, selecione uma empresa para excluir.");
        warn.setHeaderText("No Selection / Nenhuma Seleção");
        warn.showAndWait();
    }

    private Stage createStage(Stage parentStage, VBox layout) {
        Stage stage = new Stage();
        stage.setTitle(AppConstants.APP_TITLE + " - Edit Companies");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setScene(new Scene(layout, 380, 420));
        stage.getScene().getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );
        
        // Make resizable with minimum size
        stage.setResizable(true);
        stage.setMinWidth(380);
        stage.setMinHeight(420);
        
        return stage;
    }

    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }


    private void refreshTable() {
        table.getItems().setAll(companyRates.entrySet());
    }

    private void loadRates() {
        try {
            if (Files.exists(AppConstants.RATES_PATH)) {
                companyRates.clear();
                companyRates.putAll(mapper.readValue(AppConstants.RATES_PATH.toFile(),
                        new TypeReference<Map<String, RateInfo>>() {}));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveRates() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(AppConstants.RATES_PATH.toFile(), companyRates);

            // Refresh the singleton service to make changes available immediately
            CompanyRateService.getInstance().refreshRates();

            // Show success message
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION,
                    "Companies saved successfully");
            successAlert.setHeaderText("Save Complete");
            successAlert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR,
                    "Error saving companies: " + e.getMessage());
            errorAlert.setHeaderText("Save Error");
            errorAlert.showAndWait();
        }
    }

    private void openEditor(String existingName) {
        Dialog<Map.Entry<String, RateInfo>> dialog = new Dialog<>();
        dialog.setTitle(existingName == null ? "Add Company" : "Edit Company");

        GridPane grid = createEditorGrid(existingName);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = (TextField) grid.getChildren().get(1);
        TextField rateField = (TextField) grid.getChildren().get(3);
        ComboBox<String> typeCombo = (ComboBox<String>) grid.getChildren().get(5);

        setupDialogResultConverter(dialog, nameField, rateField, typeCombo);

        Optional<Map.Entry<String, RateInfo>> result = dialog.showAndWait();
        result.ifPresent(entry -> handleEditorResult(existingName, entry));
    }

    private GridPane createEditorGrid(String existingName) {
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(existingName != null ? existingName : "");

        Label rateLabel = new Label("Rate ($):");
        TextField rateField = new TextField();
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("hour", "minute");

        initializeFieldValues(existingName, rateField, typeCombo);

        Label typeLabel = new Label("Type:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(rateLabel, 0, 1);
        grid.add(rateField, 1, 1);
        grid.add(typeLabel, 0, 2);
        grid.add(typeCombo, 1, 2);

        return grid;
    }

    private void initializeFieldValues(String existingName, TextField rateField, ComboBox<String> typeCombo) {
        if (existingName != null) {
            RateInfo info = companyRates.get(existingName);
            rateField.setText(String.format(Locale.US, "%.2f", info.getValor()));
            typeCombo.setValue(info.getTipo().equals("hora") ? "hour" : "minute");
        } else {
            typeCombo.setValue("hour");
        }
    }

    private void setupDialogResultConverter(Dialog<Map.Entry<String, RateInfo>> dialog,
                                           TextField nameField, TextField rateField, ComboBox<String> typeCombo) {
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return validateAndCreateEntry(nameField, rateField, typeCombo);
            }
            return null;
        });
    }

    private Map.Entry<String, RateInfo> validateAndCreateEntry(TextField nameField, TextField rateField, ComboBox<String> typeCombo) {
        String name = nameField.getText().trim();
        String rateText = rateField.getText().trim();
        String tipo = typeCombo.getValue().equals("hour") ? "hora" : "minuto";
        
        if (!name.isEmpty() && rateText.matches("\\d*(\\.\\d{1,2})?")) {
            double rate = Double.parseDouble(rateText);
            return Map.entry(name, new RateInfo(rate, tipo));
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    AppConstants.ERROR_INVALID_RATE_EN + "\n" + AppConstants.ERROR_INVALID_RATE_PT);
            alert.showAndWait();
            return null;
        }
    }

    private void handleEditorResult(String existingName, Map.Entry<String, RateInfo> entry) {
        String newName = entry.getKey();
        RateInfo newInfo = entry.getValue();

        handleCompanyRename(existingName, newName);
        companyRates.put(newName, newInfo);
        refreshTable();
    }

    private void handleCompanyRename(String existingName, String newName) {
        if (existingName != null && !existingName.equals(newName)) {
            companyRates.remove(existingName);
            
            try {
                CompanyManagerService service = new CompanyManagerService();
                service.initialize();
                renameCompanyInAllLogs(service, existingName, newName);
            } catch (Exception ex) {
                System.err.println("Failed to rename company in logs: " + ex.getMessage());
                ex.printStackTrace();
                showRenameErrorAlert(ex);
            }
        }
    }

    private void showRenameErrorAlert(Exception ex) {
        Alert errorAlert = new Alert(Alert.AlertType.WARNING,
                "Company rate updated but failed to rename in existing logs: " + ex.getMessage());
        errorAlert.setHeaderText("Partial Update");
        errorAlert.showAndWait();
    }

    /**
     * NEW METHOD: Rename company in all log files
     */
    private void renameCompanyInAllLogs(CompanyManagerService service, String oldName, String newName) {
        try {
            List<RegistroTrabalho> allLogs = service.getRegistros();
            boolean hasChanges = updateCompanyNamesInLogs(allLogs, oldName, newName);

            if (hasChanges) {
                saveUpdatedLogsGroupedByMonth(service, allLogs);
                System.out.println("✅ Successfully renamed company '" + oldName + "' to '" + newName + "' in all logs");
            }
        } catch (Exception e) {
            throw new CompanyOperationException("Failed to rename company in logs", e);
        }
    }

    private boolean updateCompanyNamesInLogs(List<RegistroTrabalho> allLogs, String oldName, String newName) {
        boolean hasChanges = false;
        for (RegistroTrabalho log : allLogs) {
            if (oldName.equals(log.getEmpresa())) {
                log.setEmpresa(newName);
                hasChanges = true;
            }
        }
        return hasChanges;
    }

    private void saveUpdatedLogsGroupedByMonth(CompanyManagerService service, List<RegistroTrabalho> allLogs) throws Exception {
        Map<String, List<RegistroTrabalho>> monthlyLogs = groupLogsByMonth(allLogs);
        clearExistingMonthlyFiles(service, monthlyLogs);
        saveUpdatedMonthlyFiles(service, monthlyLogs);
    }

    private Map<String, List<RegistroTrabalho>> groupLogsByMonth(List<RegistroTrabalho> allLogs) {
        Map<String, List<RegistroTrabalho>> monthlyLogs = new HashMap<>();
        for (RegistroTrabalho log : allLogs) {
            String yearMonth = DateUtils.getYearMonthKeyFromDateString(log.getData());
            if (yearMonth != null) {
                monthlyLogs.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(log);
            }
        }
        return monthlyLogs;
    }

    private void clearExistingMonthlyFiles(CompanyManagerService service, Map<String, List<RegistroTrabalho>> monthlyLogs) throws Exception {
        for (String yearMonth : monthlyLogs.keySet()) {
            service.saveWorkLogsForMonth(yearMonth, new ArrayList<>());
        }
    }

    private void saveUpdatedMonthlyFiles(CompanyManagerService service, Map<String, List<RegistroTrabalho>> monthlyLogs) throws Exception {
        for (Map.Entry<String, List<RegistroTrabalho>> entry : monthlyLogs.entrySet()) {
            String yearMonth = entry.getKey();
            List<RegistroTrabalho> monthLogs = entry.getValue();
            service.saveWorkLogsForMonth(yearMonth, monthLogs);
        }
    }}
