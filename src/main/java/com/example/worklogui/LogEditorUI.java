package com.example.worklogui;

import com.example.worklogui.utils.FilterHelper;
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
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Updated LogEditorUI that works with the new file handling system
 */
public class LogEditorUI {

    private Runnable onCloseCallback;
    private BiConsumer<String, String> onFilterCallback;
    private CompanyManagerService service;

    /**
     * Set the service to use for data operations
     */
    public void setService(CompanyManagerService service) {
        this.service = service;
    }

    /**
     * Set the callback for when the editor is closed without explicit filtering
     */
    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Set the callback for when a filter update is needed
     */
    public void setOnFilterCallback(BiConsumer<String, String> callback) {
        this.onFilterCallback = callback;
    }

    /**
     * Show the log editor with the specified filter values
     */
    public void show(Stage owner, String year, String month, String company) {
        show(owner, year, month, company, null);
    }

    /**
     * Show the log editor with the specified filter values and service
     */
    public void show(Stage owner, String year, String month, String company, CompanyManagerService serviceParam) {
        try {
            // Use provided service or create new one
            if (serviceParam != null) {
                this.service = serviceParam;
            } else if (this.service == null) {
                this.service = new CompanyManagerService();
                this.service.initialize();
            }

            System.out.println("Opening log editor with filters: year=" + year + ", month=" + month + ", company=" + company);

            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("log-editor.fxml"));
            Parent root = loader.load();

            // Get the controller
            LogEditorController controller = loader.getController();

            // Set the service in the controller
            controller.setService(this.service);

            // Load all logs and apply filters
            List<RegistroTrabalho> allLogs = this.service.getRegistros();
            List<RegistroTrabalho> filteredLogs = FilterHelper.applyFilters(allLogs, year, month, company);

            // Sort logs by date (newest first)
            filteredLogs.sort((a, b) -> {
                try {
                    LocalDate dateA = LocalDate.parse(a.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                    LocalDate dateB = LocalDate.parse(b.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                    return dateB.compareTo(dateA); // Newest first
                } catch (Exception e) {
                    return 0;
                }
            });

            System.out.println("Filtered logs: " + filteredLogs.size());

            // Set the filtered and sorted logs
            controller.setRegistros(filteredLogs);

            // Pass filter values to controller
            controller.setFilterValues(year, month, company);

            // Set callbacks
            controller.setOnSaveCallback(() -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                } else {
                }
            });

            // Set filter callback for updating filter dropdowns with new years/months
            controller.setOnFilterCallback((newYear, newMonth) -> {
                if (onFilterCallback != null) {
                    onFilterCallback.accept(newYear, newMonth);
                } else {
                }
            });

            // Create and show the window
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setTitle("Work Log Editor - Filtered View");
            stage.setScene(new Scene(root));
            
            // Make resizable with minimum size
            stage.setResizable(true);
            stage.setMinWidth(600);
            stage.setMinHeight(400);
            
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "❌ Error opening Log Editor: " + e.getMessage() + "\n" +
                            "❌ Erro ao abrir o Editor de Registros: " + e.getMessage())
                    .showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "❌ Unexpected error: " + e.getMessage() + "\n" +
                            "❌ Erro inesperado: " + e.getMessage())
                    .showAndWait();
        }
    }

    /**
     * Keep the original method for backward compatibility
     */
    public void show(Stage owner) {
        show(owner, "All", "All", "All", null);
    }


}