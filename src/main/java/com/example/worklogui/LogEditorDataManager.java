package com.example.worklogui;

import com.example.worklogui.exceptions.WorkLogNotFoundException;
import com.example.worklogui.exceptions.WorkLogServiceException;
import com.example.worklogui.utils.DateUtils;
import com.example.worklogui.utils.FilterHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LogEditorDataManager {
    
    private List<RegistroTrabalho> registros = new ArrayList<>();
    private CompanyManagerService service;
    
    // Filter tracking
    private String currentFilterYear;
    private String currentFilterMonth;
    private String currentFilterCompany;
    
    public void setService(CompanyManagerService service) {
        this.service = service;
    }
    
    public void setRegistros(List<RegistroTrabalho> registros) {
        this.registros = new ArrayList<>(registros);
    }
    
    public void setFilterValues(String year, String month, String company) {
        this.currentFilterYear = year;
        this.currentFilterMonth = month;
        this.currentFilterCompany = company;
    }
    
    public String getCurrentFilterYear() {
        return currentFilterYear;
    }
    
    public String getCurrentFilterMonth() {
        return currentFilterMonth;
    }
    
    public String getCurrentFilterCompany() {
        return currentFilterCompany;
    }
    
    public List<RegistroTrabalho> getRegistros() {
        return registros;
    }
    
    public CompanyManagerService getService() {
        return service;
    }
    
    public void setupTableColumns(TableView<RegistroTrabalho> logTable) {
        logTable.getColumns().clear();
        
        setupDateColumn(logTable);
        setupCompanyColumn(logTable);
        setupHoursColumn(logTable);
        setupMinutesColumn(logTable);
        setupDoublePayColumn(logTable);
        setupEarningsColumn(logTable);
        
        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    private void setupDateColumn(TableView<RegistroTrabalho> logTable) {
        TableColumn<RegistroTrabalho, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> {
            try {
                LocalDate parsed = DateUtils.parseDisplayDate(cellData.getValue().getData());
                return new SimpleStringProperty(DateUtils.formatDisplayDate(parsed));
            } catch (Exception e) {
                return new SimpleStringProperty(cellData.getValue().getData());
            }
        });
        dateCol.setPrefWidth(100);
        logTable.getColumns().add(dateCol);
    }
    
    private void setupCompanyColumn(TableView<RegistroTrabalho> logTable) {
        TableColumn<RegistroTrabalho, String> companyCol = new TableColumn<>("Company");
        companyCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getEmpresa()));
        companyCol.setPrefWidth(120);
        logTable.getColumns().add(companyCol);
    }
    
    private void setupHoursColumn(TableView<RegistroTrabalho> logTable) {
        TableColumn<RegistroTrabalho, String> hoursCol = new TableColumn<>("Hours");
        hoursCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                String.format("%.2f", cellData.getValue().getHoras())));
        hoursCol.setPrefWidth(70);
        logTable.getColumns().add(hoursCol);
    }
    
    private void setupMinutesColumn(TableView<RegistroTrabalho> logTable) {
        TableColumn<RegistroTrabalho, String> minutesCol = new TableColumn<>("Minutes");
        minutesCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                String.format("%.0f", cellData.getValue().getMinutos())));
        minutesCol.setPrefWidth(70);
        logTable.getColumns().add(minutesCol);
    }
    
    private void setupDoublePayColumn(TableView<RegistroTrabalho> logTable) {
        TableColumn<RegistroTrabalho, String> doublePayCol = new TableColumn<>("Double Pay");
        doublePayCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().isPagamentoDobrado() ? "Yes" : "No"));
        doublePayCol.setPrefWidth(80);
        logTable.getColumns().add(doublePayCol);
    }
    
    private void setupEarningsColumn(TableView<RegistroTrabalho> logTable) {
        TableColumn<RegistroTrabalho, String> earningsCol = new TableColumn<>("Earnings");
        earningsCol.setCellValueFactory(cellData -> {
            RegistroTrabalho registro = cellData.getValue();
            try {
                double earnings = service.calculateEarnings(registro);
                return new SimpleStringProperty(String.format("$%.2f", earnings));
            } catch (Exception e) {
                return new SimpleStringProperty("N/A");
            }
        });
        earningsCol.setPrefWidth(80);
        logTable.getColumns().add(earningsCol);
    }
    
    public void clearFilteredLogs(AtomicInteger removedCount) throws Exception {
        clearDisplayedLogs(removedCount);
    }
    
    public void clearAllLogs(AtomicInteger removedCount) throws Exception {
        clearDisplayedLogs(removedCount);
    }
    
    private void clearDisplayedLogs(AtomicInteger removedCount) throws Exception {
        // Work with a copy of the current displayed logs to avoid concurrent modification
        List<RegistroTrabalho> logsToDelete = new ArrayList<>(registros);
        int deletedCount = 0;
        List<RegistroTrabalho> currentLogs = service.getRegistros();
        
        // Delete each entry by finding matching entries in current data
        for (RegistroTrabalho log : logsToDelete) {
            try {
                RegistroTrabalho toDelete = findMatchingEntry(currentLogs, log);
                if (toDelete != null) {
                    service.deleteRegistro(toDelete);
                    deletedCount++;
                }
            } catch (Exception e) {
                // Log the error but continue with other deletions
                System.err.println("Failed to delete entry: " + e.getMessage());
            }
        }
        
        removedCount.set(deletedCount);
        registros.clear();
    }
    
    private RegistroTrabalho findMatchingEntry(List<RegistroTrabalho> currentLogs, RegistroTrabalho target) {
        for (RegistroTrabalho currentLog : currentLogs) {
            if (isSameEntry(currentLog, target)) {
                return currentLog;
            }
        }
        return null;
    }
    
    public void deleteEntry(RegistroTrabalho selected) throws Exception {
        // Find the entry in the current service data by matching key fields
        List<RegistroTrabalho> currentLogs = service.getRegistros();
        RegistroTrabalho toDelete = findMatchingEntry(currentLogs, selected);
        
        if (toDelete != null) {
            service.deleteRegistro(toDelete);
            registros.remove(selected);
        } else {
            throw new WorkLogNotFoundException("Entry not found in current data - it may have been already deleted or modified");
        }
    }
    
    private boolean isSameEntry(RegistroTrabalho log1, RegistroTrabalho log2) {
        return log1.getData().equals(log2.getData()) &&
               log1.getEmpresa().equals(log2.getEmpresa()) &&
               Double.compare(log1.getHoras(), log2.getHoras()) == 0 &&
               Double.compare(log1.getMinutos(), log2.getMinutos()) == 0 &&
               log1.isPagamentoDobrado() == log2.isPagamentoDobrado();
    }
    
    public void saveRegistrosToService() throws Exception {
        // This method is no longer needed - updates are handled directly 
        // through CompanyManagerService.updateWorkLog() in LogEditorCommands
        // Keeping for backward compatibility but marking as deprecated
    }
    
}