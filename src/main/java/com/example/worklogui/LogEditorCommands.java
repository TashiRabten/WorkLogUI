package com.example.worklogui;

import com.example.worklogui.exceptions.WorkLogNotFoundException;
import com.example.worklogui.exceptions.WorkLogServiceException;
import com.example.worklogui.services.WorkLogBusinessService;
import com.example.worklogui.utils.DateUtils;
import com.example.worklogui.utils.ErrorHandler;
import com.example.worklogui.utils.ProgressDialog;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class LogEditorCommands {
    
    private final LogEditorController controller;
    private final LogEditorDataManager dataManager;
    
    public LogEditorCommands(LogEditorController controller, LogEditorDataManager dataManager) {
        this.controller = controller;
        this.dataManager = dataManager;
    }
    
    public void editLogEntry(RegistroTrabalho entry) {
        
        Dialog<RegistroTrabalho> dialog = new Dialog<>();
        dialog.setTitle("Edit Work Log Entry");
        dialog.setHeaderText("Modify the work log entry details");
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = createEditGrid(entry);
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(() -> grid.requestFocus());
        
        DatePicker datePicker = (DatePicker) grid.getChildren().get(1);
        ComboBox<String> companyCombo = (ComboBox<String>) grid.getChildren().get(3);
        TextField hoursField = (TextField) grid.getChildren().get(5);
        TextField minutesField = (TextField) grid.getChildren().get(7);
        CheckBox doublePayBox = (CheckBox) grid.getChildren().get(8);
        
        setupDialogResultConverter(dialog, datePicker, companyCombo, hoursField, minutesField, doublePayBox, saveButtonType, entry);
        
        Optional<RegistroTrabalho> result = dialog.showAndWait();
        
        result.ifPresent(updatedEntry -> {
            LocalDate originalDate = DateUtils.parseDisplayDate(entry.getData());
            handleEditResult(entry, updatedEntry, originalDate);
        });
    }
    
    private GridPane createEditGrid(RegistroTrabalho entry) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(DateUtils.parseDisplayDate(entry.getData()));
        
        ComboBox<String> companyCombo = new ComboBox<>();
        Set<String> companies = dataManager.getService().getCompanies();
        companyCombo.getItems().addAll(companies);
        
        // Don't pre-select the JSON company name - let user choose from local companies
        // If the JSON company exists locally, pre-select it; otherwise leave empty for user selection
        if (companies.contains(entry.getEmpresa())) {
            companyCombo.setValue(entry.getEmpresa());
        } else {
            companyCombo.setPromptText("Select local company for: " + entry.getEmpresa());
        }
        companyCombo.setEditable(true);
        
        TextField hoursField = new TextField(String.valueOf(entry.getHoras()));
        TextField minutesField = new TextField(String.valueOf(entry.getMinutos()));
        CheckBox doublePayBox = new CheckBox();
        doublePayBox.setSelected(entry.isPagamentoDobrado());
        
        addFieldsToGrid(grid, datePicker, companyCombo, hoursField, minutesField, doublePayBox);
        
        return grid;
    }
    
    private void addFieldsToGrid(GridPane grid, DatePicker datePicker, ComboBox<String> companyCombo,
                                TextField hoursField, TextField minutesField, CheckBox doublePayBox) {
        grid.add(new Label("Date:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Company:"), 0, 1);
        grid.add(companyCombo, 1, 1);
        grid.add(new Label("Hours:"), 0, 2);
        grid.add(hoursField, 1, 2);
        grid.add(new Label("Minutes:"), 0, 3);
        grid.add(minutesField, 1, 3);
        grid.add(doublePayBox, 1, 4);
        grid.add(new Label("Double Pay"), 0, 4);
    }
    
    private void setupDialogResultConverter(Dialog<RegistroTrabalho> dialog, DatePicker datePicker,
                                          ComboBox<String> companyCombo, TextField hoursField, 
                                          TextField minutesField, CheckBox doublePayBox, ButtonType saveButtonType, RegistroTrabalho originalEntry) {
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createUpdatedEntryFromDialog(datePicker, companyCombo, hoursField, minutesField, doublePayBox, originalEntry);
            }
            return null;
        });
    }
    
    private RegistroTrabalho createUpdatedEntryFromDialog(DatePicker datePicker, ComboBox<String> companyCombo,
                                                         TextField hoursField, TextField minutesField,
                                                         CheckBox doublePayBox, RegistroTrabalho originalEntry) {
        try {
            String dateStr = DateUtils.formatDisplayDate(datePicker.getValue());
            String company = companyCombo.getValue();
            double hours = Double.parseDouble(hoursField.getText());
            double minutes = Double.parseDouble(minutesField.getText());
            boolean doublePay = doublePayBox.isSelected();
            
            if (!validateDialogInput(company, hours, minutes)) {
                return null;
            }
            
            return buildUpdatedEntry(dateStr, company, hours, minutes, doublePay, originalEntry);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter valid numbers for hours and minutes.");
            return null;
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
            return null;
        }
    }
    
    private boolean validateDialogInput(String company, double hours, double minutes) {
        if (company == null || company.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Company name cannot be empty.");
            return false;
        }
        
        if (hours < 0 || minutes < 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Hours and minutes must be non-negative.");
            return false;
        }
        
        return true;
    }
    
    private RegistroTrabalho buildUpdatedEntry(String dateStr, String company, double hours, double minutes, boolean doublePay, RegistroTrabalho originalEntry) {
        // Get the rate info for the selected company (user maps JSON company to local company)
        RateInfo info = CompanyRateService.getInstance().getRateInfoMap()
                .getOrDefault(company, new RateInfo(0.0, "hora"));
        
        RegistroTrabalho updatedEntry = new RegistroTrabalho();
        updatedEntry.setData(dateStr);
        updatedEntry.setEmpresa(company); // Use the selected local company name
        updatedEntry.setPagamentoDobrado(doublePay);
        updatedEntry.setTaxaUsada(info.getValor()); // Use the selected company's rate
        updatedEntry.setTipoUsado(info.getTipo()); // Use the selected company's type
        
        // Apply the selected company's type logic to determine which field to use
        if (info.getTipo().equalsIgnoreCase("minuto")) {
            updatedEntry.setHoras(0);
            updatedEntry.setMinutos(minutes); // Use the user-edited minutes value
        } else {
            updatedEntry.setHoras(hours); // Use the user-edited hours value
            updatedEntry.setMinutos(0);
        }
        
        return updatedEntry;
    }
    
    private void handleEditResult(RegistroTrabalho entry, RegistroTrabalho updatedEntry, LocalDate originalDate) {
        try {
            LocalDate newDate = DateUtils.parseDisplayDate(updatedEntry.getData());
            boolean dateChanged = !originalDate.equals(newDate);
            
            // Use the service's business service directly (like original working version)
            CompanyManagerService service = dataManager.getService();
            if (service != null) {
                service.updateWorkLog(entry, updatedEntry);
            }
            
            // Update the local list directly with simple index replacement (like original)
            List<RegistroTrabalho> registros = dataManager.getRegistros();
            int index = registros.indexOf(entry);
            if (index >= 0) {
                registros.set(index, updatedEntry);
            }
            
            // Refresh table immediately (no Platform.runLater wrapping)
            controller.refreshLogTable();
            
            // Handle callbacks immediately like the original version
            if (dateChanged) {
                BiConsumer<String, String> filterCallback = controller.getOnFilterCallback();
                if (filterCallback != null) {
                    String newYear = String.valueOf(newDate.getYear());
                    String newMonth = String.format("%02d", newDate.getMonthValue());
                    filterCallback.accept(newYear, newMonth);
                    
                    // If date changed, close the LogEditor so user can see the main window highlight
                    Platform.runLater(() -> {
                        controller.onClose();
                    });
                }
            } else {
                // For same-date edits, highlight the entry in the LogEditor and call save callback
                Platform.runLater(() -> {
                    controller.scrollToAndHighlightEntry(updatedEntry);
                });
                
                Runnable saveCallback = controller.getOnSaveCallback();
                if (saveCallback != null) {
                    saveCallback.run();
                }
                
                // Notify the main window about the edited entry
                java.util.function.Consumer<RegistroTrabalho> entryEditedCallback = controller.getOnEntryEditedCallback();
                if (entryEditedCallback != null) {
                    entryEditedCallback.accept(updatedEntry);
                }
            }
            
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("Error updating entry", e);
        }
    }
    
    
    
    public void performClearLogsInBackground(boolean hasFilter) {
        try {
            CompanyManagerService service = dataManager.getService();
            List<RegistroTrabalho> registros = dataManager.getRegistros();
            
            if (hasFilter) {
                clearFilteredLogs(service, registros);
            } else {
                clearAllLogs(service, registros);
            }
            
            // Refresh table immediately
            controller.refreshLogTable();
            
            // Call save callback
            Runnable saveCallback = controller.getOnSaveCallback();
            if (saveCallback != null) {
                saveCallback.run();
            }
            
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("Error clearing logs", e);
            showAlert(Alert.AlertType.ERROR, "Clear Failed", "Failed to clear logs: " + e.getMessage());
        }
    }
    
    private void clearFilteredLogs(CompanyManagerService service, List<RegistroTrabalho> registros) {
        List<RegistroTrabalho> logsToDelete = new ArrayList<>(registros);
        int removedCount = deleteLogs(service, logsToDelete);
        registros.clear();
        showClearCompleteMessage(removedCount, true);
    }
    
    private void clearAllLogs(CompanyManagerService service, List<RegistroTrabalho> registros) {
        List<RegistroTrabalho> logsToDelete = new ArrayList<>(registros);
        int removedCount = deleteLogs(service, logsToDelete);
        registros.clear();
        showClearCompleteMessage(removedCount, false);
    }
    
    private int deleteLogs(CompanyManagerService service, List<RegistroTrabalho> logsToDelete) {
        int removedCount = logsToDelete.size();
        
        for (RegistroTrabalho log : logsToDelete) {
            try {
                if (service != null) {
                    service.deleteRegistro(log);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete log: " + e.getMessage());
                removedCount--; // Reduce count if deletion failed
            }
        }
        
        return removedCount;
    }
    
    private void showClearCompleteMessage(int removedCount, boolean hasFilter) {
        String message = hasFilter 
            ? String.format("Cleared %d filtered entries.", removedCount)
            : String.format("Cleared %d entries.", removedCount);
        
        showAlert(Alert.AlertType.INFORMATION, "Clear Complete", message);
    }
    
    public void performDeleteInBackground(RegistroTrabalho selected) {
        try {
            // Use direct service call like the original working version
            CompanyManagerService service = dataManager.getService();
            if (service != null) {
                service.deleteRegistro(selected);
            }
            
            // Remove ONLY the selected entry from local list
            List<RegistroTrabalho> registros = dataManager.getRegistros();
            boolean removed = registros.remove(selected);
            
            System.out.println("Single delete: removed=" + removed + ", remaining entries=" + registros.size());
            
            // Refresh table immediately
            controller.refreshLogTable();
            
            // Call save callback
            Runnable saveCallback = controller.getOnSaveCallback();
            if (saveCallback != null) {
                saveCallback.run();
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Log entry deleted successfully.");
            
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("Error deleting entry", e);
            showAlert(Alert.AlertType.ERROR, "Delete Failed", "Failed to delete the entry: " + e.getMessage());
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