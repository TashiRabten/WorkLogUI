
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

/**
 * Updated LogEditorUI that properly sets up callbacks for filter updates
 */
public class LogEditorUI {

    private Runnable onCloseCallback;
    private BiConsumer<String, String> onFilterCallback;

    /**
     * Set the callback for when the editor is closed without explicit filtering
     */
    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Set the callback for when a filter update is needed
     * This callback is used to update the year/month filters in the main UI
     */
    public void setOnFilterCallback(BiConsumer<String, String> callback) {
        this.onFilterCallback = callback;
    }

    /**
     * Show the log editor with the specified filter values
     */
    public void show(Stage owner, String year, String month, String company) {
        try {
            System.out.println("Opening log editor with filters: year=" + year + ", month=" + month + ", company=" + company);

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
                    System.err.println("Skipping log with invalid date: " + log.getData());
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

            System.out.println("Filtered logs: " + filteredLogs.size());

            // Set the filtered and sorted logs
            controller.setRegistros(filteredLogs);

            // IMPORTANT: Pass filter values to controller
            controller.setFilterValues(year, month, company);

            // Set callbacks
            controller.setOnSaveCallback(() -> {
                if (onCloseCallback != null) {
                    System.out.println("Running onCloseCallback from LogEditorUI");
                    onCloseCallback.run();
                }
            });

            // Set filter callback - CRITICAL for updating filter dropdowns with new years/months
            controller.setOnFilterCallback((newYear, newMonth) -> {
                if (onFilterCallback != null) {
                    System.out.println("Running onFilterCallback with year=" + newYear + ", month=" + newMonth);
                    onFilterCallback.accept(newYear, newMonth);
                }
            });

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