package com.example.worklogui;

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

        TableColumn<Map.Entry<String, RateInfo>, String> nameCol = new TableColumn<>("Company");
        nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getKey()));
        nameCol.setPrefWidth(180);

        TableColumn<Map.Entry<String, RateInfo>, String> rateCol = new TableColumn<>("Rate");
        rateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                String.format(Locale.US, "$ %.2f (%s)",
                        param.getValue().getValue().getValor(),
                        param.getValue().getValue().getTipo())
        ));
        rateCol.setPrefWidth(150);

        table.getColumns().addAll(nameCol, rateCol);
        refreshTable();

        Button addBtn = new Button("Add");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button saveBtn = new Button("Save");
        Button closeBtn = new Button("Close");


        addBtn.setOnAction(e -> openEditor(null));
        editBtn.setOnAction(e -> {
            Map.Entry<String, RateInfo> selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) openEditor(selected.getKey());
        });
        deleteBtn.setOnAction(e -> {
            Map.Entry<String, RateInfo> selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Company / Excluir Empresa");
                confirm.setHeaderText("Are you sure you want to delete this company?\nTem certeza de que deseja excluir esta empresa?");
                confirm.setContentText("This action cannot be undone.\nEssa ação não pode ser desfeita.");

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    companyRates.remove(selected.getKey());
                    refreshTable();
                    saveRates(); // ✅ Only save if confirmed
                }
            } else {
                Alert warn = new Alert(Alert.AlertType.WARNING,
                        "Please select a company to delete.\nPor favor, selecione uma empresa para excluir.");
                warn.setHeaderText("No Selection / Nenhuma Seleção");
                warn.showAndWait();
            }
        });

        saveBtn.setOnAction(e -> {
            saveRates();
            refreshTable(); // ✅ Now visibly updates the table
        });
        closeBtn.setOnAction(e -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });

        HBox buttons = new HBox(10, addBtn, editBtn, deleteBtn, saveBtn, closeBtn);
        buttons.setPadding(new Insets(10));

        VBox layout = new VBox(10, table, buttons);
        layout.setPadding(new Insets(15));

        Stage stage = new Stage();
        stage.setTitle(AppConstants.APP_TITLE + " - Edit Companies");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setScene(new Scene(layout, 380, 420));
        stage.getScene().getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        stage.show();
        stage.setOnHiding(e -> {
            if (onCloseCallback != null) onCloseCallback.run();
        });

    }

    private void openEditor(String existingName) {
        Dialog<Map.Entry<String, RateInfo>> dialog = new Dialog<>();
        dialog.setTitle(existingName == null ? "Add Company" : "Edit Company");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(existingName != null ? existingName : "");

        Label rateLabel = new Label("Rate ($):");
        TextField rateField = new TextField();
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("hora", "minuto");

        if (existingName != null) {
            RateInfo info = companyRates.get(existingName);
            rateField.setText(String.format(Locale.US, "%.2f", info.getValor()));
            typeCombo.setValue(info.getTipo());
        } else {
            typeCombo.setValue("hora");
        }

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

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String name = nameField.getText().trim();
                String rateText = rateField.getText().trim();
                String tipo = typeCombo.getValue();
                if (!name.isEmpty() && rateText.matches("\\d*(\\.\\d{1,2})?"))
                {
                    double rate = Double.parseDouble(rateText);
                    return Map.entry(name, new RateInfo(rate, tipo));
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            AppConstants.ERROR_INVALID_RATE_EN + "\n" + AppConstants.ERROR_INVALID_RATE_PT);
                    alert.showAndWait();
                }
            }
            return null;
        });

        Optional<Map.Entry<String, RateInfo>> result = dialog.showAndWait();
        result.ifPresent(entry -> {
            String newName = entry.getKey();
            RateInfo newInfo = entry.getValue();

            if (existingName != null && !existingName.equals(newName)) {
                companyRates.remove(existingName);

                // ✅ Rename in saved log file
                try {
                    Path worklogPath = AppConstants.WORKLOG_PATH;
                    List<RegistroTrabalho> logs = FileLoader.carregarRegistros(worklogPath);
                    for (RegistroTrabalho r : logs) {
                        if (r.getEmpresa().equals(existingName)) {
                            r.setEmpresa(newName);
                        }
                    }
                    FileLoader.salvarRegistros(worklogPath, logs);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            companyRates.put(newName, newInfo);
            refreshTable();
        });


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
    }
