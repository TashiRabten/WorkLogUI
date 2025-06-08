package com.example.worklogui;

import com.example.worklogui.utils.FilterHelper;
import com.example.worklogui.utils.DateUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.time.LocalDate;
import java.util.List;
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

    private LogEditorCommands commands;
    private LogEditorDataManager dataManager;
    private Runnable onSaveCallback;
    private BiConsumer<String, String> onFilterCallback;
    private java.util.function.Consumer<RegistroTrabalho> onEntryEditedCallback;

    public void setService(CompanyManagerService service) {
        this.dataManager = new LogEditorDataManager();
        this.dataManager.setService(service);
        this.commands = new LogEditorCommands(this, dataManager);
        setupTableIfReady();
    }

    public void setRegistros(List<RegistroTrabalho> registros) {
        if (dataManager != null) {
            dataManager.setRegistros(registros);
            if (logTable != null) {
                refreshLogTable();
            }
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setFilterValues(String year, String month, String company) {
        if (dataManager != null) {
            dataManager.setFilterValues(year, month, company);
        }
    }

    public void setOnFilterCallback(BiConsumer<String, String> callback) {
        this.onFilterCallback = callback;
    }
    
    public void setOnEntryEditedCallback(java.util.function.Consumer<RegistroTrabalho> callback) {
        this.onEntryEditedCallback = callback;
    }

    @FXML
    public void initialize() {
        // Initialize will be called after setService() sets up the dataManager
        setupRowClickHandler();
    }
    
    private void setupTableIfReady() {
        if (dataManager != null && logTable != null) {
            dataManager.setupTableColumns(logTable);
        }
    }

    public void refreshLogTable() {
        if (logTable != null && dataManager != null) {
            Platform.runLater(() -> {
                logTable.getItems().clear();
                logTable.getItems().addAll(dataManager.getRegistros());
            });
        }
    }
    
    public void scrollToMostRecentEntry() {
        if (logTable == null || logTable.getItems().isEmpty()) return;
        
        Platform.runLater(() -> {
            // Find the most recent entry by date
            RegistroTrabalho newestEntry = null;
            for (RegistroTrabalho entry : logTable.getItems()) {
                if (newestEntry == null || isMoreRecent(entry, newestEntry)) {
                    newestEntry = entry;
                }
            }
            
            if (newestEntry != null) {
                logTable.getSelectionModel().select(newestEntry);
                logTable.scrollTo(newestEntry);
            }
        });
    }
    
    public void scrollToAndHighlightEntry(RegistroTrabalho targetEntry) {
        if (logTable == null || logTable.getItems().isEmpty()) return;
        
        // Find the entry in the table and highlight it
        for (RegistroTrabalho entry : logTable.getItems()) {
            if (isSameEntry(entry, targetEntry)) {
                logTable.getSelectionModel().select(entry);
                logTable.scrollTo(entry);
                logTable.requestFocus();
                break;
            }
        }
    }
    
    private boolean isSameEntry(RegistroTrabalho entry1, RegistroTrabalho entry2) {
        return entry1.getData().equals(entry2.getData()) &&
               entry1.getEmpresa().equals(entry2.getEmpresa()) &&
               Double.compare(entry1.getHoras(), entry2.getHoras()) == 0 &&
               Double.compare(entry1.getMinutos(), entry2.getMinutos()) == 0 &&
               entry1.isPagamentoDobrado() == entry2.isPagamentoDobrado();
    }
    
    private boolean isMoreRecent(RegistroTrabalho entry1, RegistroTrabalho entry2) {
        try {
            LocalDate date1 = DateUtils.parseDisplayDate(entry1.getData());
            LocalDate date2 = DateUtils.parseDisplayDate(entry2.getData());
            return date1.isAfter(date2);
        } catch (Exception e) {
            return false;
        }
    }
    
    private void setupRowClickHandler() {
        logTable.setRowFactory(tv -> {
            TableRow<RegistroTrabalho> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && commands != null) {
                    commands.editLogEntry(row.getItem());
                }
            });
            return row;
        });
    }

    @FXML
    public void onEditLog() {
        RegistroTrabalho selected = logTable.getSelectionModel().getSelectedItem();
        if (selected != null && commands != null) {
            commands.editLogEntry(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection / Nenhuma Seleção",
                    "Please select a log entry to edit.\nPor favor, selecione um registro para editar.");
        }
    }

    @FXML
    public void onDeleteLog() {
        RegistroTrabalho selected = logTable.getSelectionModel().getSelectedItem();
        if (selected != null && commands != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete this log entry?\nTem certeza de que deseja excluir este registro?",
                    ButtonType.YES, ButtonType.NO);
            confirmation.setHeaderText("Confirm Deletion / Confirmar Exclusão");

            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    commands.performDeleteInBackground(selected);
                }
            });
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection / Nenhuma Seleção",
                    "Please select a log entry to delete.\nPor favor, selecione um registro para excluir.");
        }
    }

    @FXML
    public void onClearAllLogs() {
        if (dataManager != null && commands != null) {
            final boolean hasFilter = FilterHelper.hasActiveFilters(
                dataManager.getCurrentFilterYear(), 
                dataManager.getCurrentFilterMonth(), 
                dataManager.getCurrentFilterCompany());

            if (confirmClearOperation(hasFilter)) {
                commands.performClearLogsInBackground(hasFilter);
            }
        }
    }

    private boolean confirmClearOperation(boolean hasFilter) {
        String title = hasFilter ? "Clear Filtered Logs / Limpar Registros Filtrados" 
                                 : "Clear All Logs / Limpar Todos os Registros";
        
        String message = hasFilter 
            ? "This will clear only the logs matching your current filter.\nIsso limpará apenas os registros que correspondem ao seu filtro atual.\n\nContinue?"
            : "This will clear ALL work log entries. This action cannot be undone.\nIsso limpará TODOS os registros de trabalho. Esta ação não pode ser desfeita.\n\nContinue?";

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        confirmation.setTitle(title);
        confirmation.setHeaderText("Confirm Clear Operation / Confirmar Operação de Limpeza");

        return confirmation.showAndWait()
                .filter(response -> response == ButtonType.YES)
                .isPresent();
    }

    @FXML
    public void onSave() {
        try {
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

    @FXML
    public void onClose() {
        try {
            // Try multiple approaches to close the window
            if (rootContainer != null && rootContainer.getScene() != null && rootContainer.getScene().getWindow() != null) {
                rootContainer.getScene().getWindow().hide();
            } else if (logTable != null && logTable.getScene() != null && logTable.getScene().getWindow() != null) {
                logTable.getScene().getWindow().hide();
            } else {
                // Last resort: try to find any stage that contains our components
                if (closeBtn != null && closeBtn.getScene() != null && closeBtn.getScene().getWindow() != null) {
                    closeBtn.getScene().getWindow().hide();
                }
            }
        } catch (Exception e) {
            // If all else fails, print error but don't crash
            System.err.println("Could not close log editor window: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Getter methods for commands class
    public Runnable getOnSaveCallback() {
        return onSaveCallback;
    }

    public BiConsumer<String, String> getOnFilterCallback() {
        return onFilterCallback;
    }
    
    public java.util.function.Consumer<RegistroTrabalho> getOnEntryEditedCallback() {
        return onEntryEditedCallback;
    }

    public CompanyManagerService getService() {
        return dataManager != null ? dataManager.getService() : null;
    }
}