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
    private java.util.function.Consumer<RegistroTrabalho> onEntryEditedCallback;
    private CompanyManagerService service;
    private Parent currentRoot;

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
     * Set the callback for when an entry is edited (but date doesn't change)
     */
    public void setOnEntryEditedCallback(java.util.function.Consumer<RegistroTrabalho> callback) {
        this.onEntryEditedCallback = callback;
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
            initializeService(serviceParam);
            LogEditorController controller = loadLogEditorController();
            List<RegistroTrabalho> filteredLogs = prepareFilteredLogs(year, month, company);
            
            setupController(controller, filteredLogs, year, month, company);
            showLogEditorStage(owner, controller);
            
        } catch (IOException e) {
            handleIOException(e);
        } catch (Exception e) {
            handleGeneralException(e);
        }
    }
    
    private void initializeService(CompanyManagerService serviceParam) throws Exception {
        if (serviceParam != null) {
            this.service = serviceParam;
        } else if (this.service == null) {
            this.service = new CompanyManagerService();
            this.service.initialize();
        }
    }
    
    private LogEditorController loadLogEditorController() throws IOException {
        System.out.println("Opening log editor with filters");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("log-editor.fxml"));
        Parent root = loader.load();
        this.currentRoot = root; // Store for stage creation
        return loader.getController();
    }
    
    private List<RegistroTrabalho> prepareFilteredLogs(String year, String month, String company) {
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
        return filteredLogs;
    }
    
    private void setupController(LogEditorController controller, List<RegistroTrabalho> filteredLogs, 
                                String year, String month, String company) {
        controller.setService(this.service);
        controller.setRegistros(filteredLogs);
        controller.setFilterValues(year, month, company);
        setupControllerCallbacks(controller);
    }
    
    private void setupControllerCallbacks(LogEditorController controller) {
        controller.setOnSaveCallback(() -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
        
        controller.setOnFilterCallback((newYear, newMonth) -> {
            if (onFilterCallback != null) {
                onFilterCallback.accept(newYear, newMonth);
            }
        });
        
        controller.setOnEntryEditedCallback((editedEntry) -> {
            if (onEntryEditedCallback != null) {
                onEntryEditedCallback.accept(editedEntry);
            }
        });
    }
    
    private void showLogEditorStage(Stage owner, LogEditorController controller) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Work Log Editor - Filtered View");
        stage.setScene(new Scene(currentRoot));
        
        stage.setResizable(true);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        
        stage.show();
    }
    
    private void handleIOException(IOException e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR,
                "❌ Error opening Log Editor: " + e.getMessage() + "\n" +
                        "❌ Erro ao abrir o Editor de Registros: " + e.getMessage())
                .showAndWait();
    }
    
    private void handleGeneralException(Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR,
                "❌ Unexpected error: " + e.getMessage() + "\n" +
                        "❌ Erro inesperado: " + e.getMessage())
                .showAndWait();
    }

    /**
     * Keep the original method for backward compatibility
     */
    public void show(Stage owner) {
        show(owner, "All", "All", "All", null);
    }


}