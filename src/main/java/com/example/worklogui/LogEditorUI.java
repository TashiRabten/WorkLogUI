package com.example.worklogui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class LogEditorUI {

    private Runnable onCloseCallback;
    private BiConsumer<String, String> onFilterCallback;

    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public void setOnFilterCallback(BiConsumer<String, String> callback) {
        this.onFilterCallback = callback;
    }

    public void show(Stage owner, String year, String month, String company) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("log-editor.fxml"));
            Parent root = loader.load();

            // Get the controller
            LogEditorController controller = loader.getController();

            // Load all logs
            List<RegistroTrabalho> allLogs = FileLoader.carregarRegistros(AppConstants.WORKLOG_PATH);
            List<RegistroTrabalho> filteredLogs = new ArrayList<>();

            // Apply filters
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            for (RegistroTrabalho log : allLogs) {
                try {
                    LocalDate date = LocalDate.parse(log.getData(), formatter);
                    boolean matches = true;

                    // Check year filter
                    if (!"All".equals(year)) {
                        matches &= String.valueOf(date.getYear()).equals(year);
                    }

                    // Check month filter
                    if (!"All".equals(month)) {
                        matches &= String.format("%02d", date.getMonthValue()).equals(month);
                    }

                    // Check company filter
                    if (!"All".equals(company)) {
                        matches &= log.getEmpresa().equals(company);
                    }

                    if (matches) {
                        filteredLogs.add(log);
                    }
                } catch (Exception e) {
                    // Skip entries with invalid dates
                }
            }

            // Sort logs by date (newest first)
            filteredLogs.sort((a, b) -> {
                try {
                    LocalDate dateA = LocalDate.parse(a.getData(), formatter);
                    LocalDate dateB = LocalDate.parse(b.getData(), formatter);
                    // For newest first (descending order)
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return 0;
                }
            });

            // Set the filtered and sorted logs
            controller.setRegistros(filteredLogs);

            // Set callbacks
            controller.setOnSaveCallback(() -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            });

            controller.setOnFilterCallback(onFilterCallback);

            // Create and show the window
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setTitle("Work Log Editor - Filtered View");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "❌ Error opening Log Editor: " + e.getMessage() + "\n" +
                            "❌ Erro ao abrir o Editor de Registros: " + e.getMessage())
                    .showAndWait();
        }
    }

    // Keep the original method for backward compatibility
    public void show(Stage owner) {
        show(owner, "All", "All", "All");
    }
}