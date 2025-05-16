package com.example.worklogui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

/**
 * Main UI controller for the WorkLog application.
 * Acts as a coordinator between various component controllers.
 */
public class CompanyManagerUI {

    // FXML controls - Work Log Entry section
    @FXML private DatePicker dateField;
    @FXML private ComboBox<String> jobTypeCombo;
    @FXML private TextField valueField;
    @FXML private CheckBox doublePayCheckBox;

    // FXML controls - Filter section
    @FXML private ComboBox<String> yearFilter;
    @FXML private ComboBox<String> monthFilter;
    @FXML private ComboBox<String> companyFilter;

    // FXML controls - Log Table section
    @FXML private TableView<DisplayEntry> logTable;
    @FXML private TableColumn<DisplayEntry, String> dateCol;
    @FXML private TableColumn<DisplayEntry, String> companyCol;
    @FXML private TableColumn<DisplayEntry, Double> hoursCol;
    @FXML private TableColumn<DisplayEntry, Double> minutesCol;
    @FXML private TableColumn<DisplayEntry, Boolean> doublePayCol;
    @FXML private TableColumn<DisplayEntry, String> earningsCol;
    @FXML private Label netTotalLabel;

    // FXML controls - Button section
    @FXML private Button editCompaniesBtn;
    @FXML private Button openLogEditorBtn;
    @FXML private Button editBillsBtn;

    // FXML controls - Status section
    @FXML private TextArea statusArea;

    // Service and component controllers
    private final CompanyManagerService service = new CompanyManagerService();
    private WorkLogEntryController workLogEntryController;
    private FilterController filterController;
    private LogTableController logTableController;
    private BillsManager billsManager;
    private ExportManager exportManager;
    private WarningManager warningManager;
    private StatusManager statusManager;

    @FXML
    public void initialize() {
        try {
            // Initialize the service
            service.initialize();

            // Create and initialize component controllers
            initializeControllers();

            // Apply initial filter
            onApplyFilter();


        } catch (Exception e) {
            statusManager.setStatusMessage("Erro ao inicializar / Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize all component controllers
     */
    private void initializeControllers() {
        // Create controllers
        warningManager = new WarningManager(service);
        statusManager = new StatusManager(statusArea, warningManager);
        workLogEntryController = new WorkLogEntryController(service);
        filterController = new FilterController(service);
        logTableController = new LogTableController(service);
        billsManager = new BillsManager(service);
        exportManager = new ExportManager(service);

        // Initialize status manager
        statusManager.initialize();

        // Set up work log entry controller
        workLogEntryController.setupControls(dateField, jobTypeCombo, valueField, doublePayCheckBox);
        workLogEntryController.setStatusMessageHandler(statusManager::setStatusMessage);
        workLogEntryController.setWarningMessageHandler(warningManager::setWarning);
        workLogEntryController.setOnWorkLoggedCallback(this::refreshAfterWorkLogged);

        // Set up filter controller
        filterController.setupControls(yearFilter, monthFilter, companyFilter);
        filterController.setStatusMessageHandler(statusManager::setStatusMessage);
        filterController.setWarningMessageHandler(warningManager::setWarning);
        filterController.setOnFilterAppliedCallback(this::refreshTableWithCurrentFilters);

        // Set up log table controller
        logTableController.setupControls(logTable, dateCol, companyCol, hoursCol, minutesCol, doublePayCol, earningsCol, netTotalLabel);
        logTableController.setStatusMessageHandler(statusManager::setStatusMessage);

        // Set up bills manager
        billsManager.setStatusMessageHandler(statusManager::setStatusMessage);
        billsManager.setWarningMessageHandler(warningManager::setWarning);
        billsManager.setOnBillsUpdatedCallback(this::refreshAfterBillsUpdated);

        // Set up export manager
        exportManager.setStatusMessageHandler(statusManager::setStatusMessage);
        exportManager.setCurrentEntriesSupplier(logTableController::getCurrentDisplayEntries);
    }

    /**
     * Refresh UI after work is logged
     */
    private void refreshAfterWorkLogged() {
        // Refresh the year/month map from scratch
        filterController.refreshYearToMonthsMap();

        // Get the new year and month values for UI update
        LocalDate now = LocalDate.now();
        String newYear = String.valueOf(now.getYear());
        String newMonth = String.format("%02d", now.getMonthValue());

        // Update filter dropdowns
        filterController.updateYearFilterItems();
        filterController.setFilterValues(newYear, newMonth, jobTypeCombo.getValue());

        // Apply the new filter
        onApplyFilter();
    }

    /**
     * Refresh UI after bills are updated
     */
    private void refreshAfterBillsUpdated() {
        filterController.refreshYearToMonthsMap();
        filterController.updateYearFilterItems();
        filterController.updateMonthFilter();
        onApplyFilter();
        Platform.runLater(logTableController::scrollToMostRecentBill);
    }

    /**
     * Refresh table with current filter values
     */
    private void refreshTableWithCurrentFilters() {
        String year = filterController.getSelectedYear();
        String month = filterController.getSelectedMonth();
        String company = filterController.getSelectedCompany();

        logTableController.updateTable(year, month, company);
        warningManager.checkFilterWarnings(year, month);
    }

    /**
     * Handle edit companies button click
     */
    @FXML
    public void handleEditCompanies() {
        CompanyEditorUI editor = new CompanyEditorUI();
        editor.setOnClose(this::reloadCompanyList);
        editor.show((Stage) editCompaniesBtn.getScene().getWindow());
    }

    /**
     * Reload company list after editing
     */
    private void reloadCompanyList() {
        workLogEntryController.reloadCompanyList();
    }

    /**
     * Handle log work button click
     */
    @FXML
    public void onLogWork() {
        workLogEntryController.onLogWork();
    }

    /**
     * Handle apply filter button click
     */
    @FXML
    public void onApplyFilter() {
        filterController.onApplyFilter();
    }

    /**
     * Handle clear filter button click
     */
    @FXML
    public void onClearFilter() {
        logTableController.clearDisplay();
        filterController.onClearFilter();
    }

    /**
     * Handle edit bills button click
     */
    @FXML
    public void onEditBills() {
        billsManager.editBills(
                (Stage) editBillsBtn.getScene().getWindow(),
                filterController.getSelectedYear(),
                filterController.getSelectedMonth(),
                filterController.getSelectedCompany()
        );
    }

    /**
     * Handle edit log entry button click
     */
    @FXML
    public void onEditLogEntry() {
        logTableController.onEditLogEntry();
    }

    /**
     * Handle delete log entry button click
     */
    @FXML
    public void onDeleteLogEntry() {
        logTableController.onDeleteLogEntry();
        // Refresh after deletion
        filterController.refreshYearToMonthsMap();
        filterController.updateYearFilterItems();
        filterController.updateMonthFilter();
        onApplyFilter();
    }

    /**
     * Handle show total button click
     */
    @FXML
    public void onShowTotal() {
        statusManager.showTimeSummary(service);
    }

    /**
     * Handle show earnings button click
     */
    @FXML
    public void onShowEarnings() {
        statusManager.showEarningsSummary(service);
    }

    /**
     * Handle show summary by month and year button click
     */
    @FXML
    public void onShowSummaryByMonthAndYear() {
        statusManager.showSummaryByMonthAndYear(service);
    }

    /**
     * Handle export excel button click
     */
    @FXML
    public void onExportExcel() {
        exportManager.exportToExcel();
    }

    /**
     * Handle open log editor button click
     */
    @FXML
    public void onOpenLogEditor() {
        LogEditorUI editor = new LogEditorUI();
        editor.setOnClose(() -> {
            try {
                service.reloadRegistros();
                filterController.refreshYearToMonthsMap();

                // Update filter dropdowns
                filterController.updateYearFilterItems();
                filterController.updateMonthFilter();
            } catch (Exception e) {
                e.printStackTrace();
            }
            onApplyFilter();
        });
        editor.show((Stage) openLogEditorBtn.getScene().getWindow());
    }
}