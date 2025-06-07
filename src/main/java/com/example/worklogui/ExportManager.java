package com.example.worklogui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages exporting data to Excel
 */
public class ExportManager {

    private final CompanyManagerService service;
    private Consumer<String> statusMessageHandler;
    private Supplier<List<DisplayEntry>> currentEntriesSupplier;
    
    public ExportManager(CompanyManagerService service) {
        this.service = service;
    }
    
    /**
     * Set callback for status messages
     */
    public void setStatusMessageHandler(Consumer<String> handler) {
        this.statusMessageHandler = handler;
    }
    
    /**
     * Set supplier for current displayed entries
     */
    public void setCurrentEntriesSupplier(Supplier<List<DisplayEntry>> supplier) {
        this.currentEntriesSupplier = supplier;
    }
    
    /**
     * Export data to Excel
     */
    public void exportToExcel() {
        try {
            Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
            choiceAlert.setTitle("Export Options");
            choiceAlert.setHeaderText("Choose export mode");
            choiceAlert.setContentText("Do you want to export all records, or only the ones currently filtered?\n\n"
                    + "Deseja exportar todos os registros ou apenas os filtrados?");

            ButtonType exportFiltered = new ButtonType("Only Filtered / Apenas Filtrados");
            ButtonType exportAll = new ButtonType("Export All / Exportar Tudo");
            ButtonType cancel = new ButtonType("Cancel / Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

            choiceAlert.getButtonTypes().setAll(exportFiltered, exportAll, cancel);

            Optional<ButtonType> result = choiceAlert.showAndWait();

            if (result.isEmpty() || result.get() == cancel) {
                setStatusMessage("❌ Export canceled.\n❌ Exportação cancelada.");
                return;
            }

            List<DisplayEntry> entriesToExport = new ArrayList<>();

            if (result.get() == exportAll) {
                performFullExport();
            } else {
                performFilteredExport();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Unexpected Error / Erro Inesperado");
            errorAlert.setHeaderText("An unexpected error occurred / Ocorreu um erro inesperado");
            errorAlert.setContentText("Error: " + ex.getMessage() + "\n\n" +
                    "Please check the console for more details.\n" +
                    "Por favor, verifique o console para mais detalhes.");
            errorAlert.showAndWait();
        }
    }
    
    /**
     * Export all data to Excel
     */
    private void performFullExport() {
        try {
            service.exportToExcel();
            setStatusMessage("✔ Exported to 'documents/worklog/exports'.\n✔ Exportado para a pasta 'documents/worklog/exports'.");

            // Show success alert with file location
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Export Successful / Exportação Bem-sucedida");
            successAlert.setHeaderText(null);
            successAlert.setContentText("Excel file exported successfully to:\n" +
                    AppConstants.EXPORT_FOLDER.toAbsolutePath() + "\n\n" +
                    "Arquivo Excel exportado com sucesso para:\n" +
                    AppConstants.EXPORT_FOLDER.toAbsolutePath());
            successAlert.showAndWait();
        } catch (IOException e) {
            handleExportError(e);
        }
    }
    
    /**
     * Export only filtered data to Excel
     */
    private void performFilteredExport() {
        if (currentEntriesSupplier == null) {
            setStatusMessage("❌ Error: Cannot access current entries.");
            return;
        }
        
        List<DisplayEntry> entriesToExport = currentEntriesSupplier.get();
        
        try {
            ExcelExporter.exportToExcel(entriesToExport, service, false);
            setStatusMessage("✔ Exported to 'documents/worklog/exports'.\n✔ Exportado para a pasta 'documents/worklog/exports'.");

            // Show success alert with file location
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Export Successful / Exportação Bem-sucedida");
            successAlert.setHeaderText(null);
            successAlert.setContentText("Excel file exported successfully to:\n" +
                    AppConstants.EXPORT_FOLDER.toAbsolutePath() + "\n\n" +
                    "Arquivo Excel exportado com sucesso para:\n" +
                    AppConstants.EXPORT_FOLDER.toAbsolutePath());
            successAlert.showAndWait();
        } catch (IOException e) {
            handleExportError(e);
        }
    }
    
    /**
     * Handle export error
     */
    private void handleExportError(IOException e) {
        setStatusMessage("❌ Export error: " + e.getMessage() + "\n❌ Erro na exportação: " + e.getMessage());
        e.printStackTrace();

        // Show detailed error alert
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Export Error / Erro de Exportação");
        errorAlert.setHeaderText("Failed to export Excel file / Falha ao exportar arquivo Excel");
        errorAlert.setContentText("Error details / Detalhes do erro:\n" + e.getMessage() +
                "\n\nExport path / Caminho de exportação:\n" +
                AppConstants.EXPORT_FOLDER.toAbsolutePath());
        errorAlert.showAndWait();
    }
    
    private void setStatusMessage(String message) {
        if (statusMessageHandler != null) {
            statusMessageHandler.accept(message);
        }
    }
}
