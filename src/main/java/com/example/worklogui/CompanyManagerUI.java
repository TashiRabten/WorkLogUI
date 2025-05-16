package com.example.worklogui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
        logTableController.setFilterController(filterController);

        // Set up export manager
        exportManager.setStatusMessageHandler(statusManager::setStatusMessage);
        exportManager.setCurrentEntriesSupplier(logTableController::getCurrentDisplayEntries);

        // In the initializeControllers method
        billsManager.setStatusMessageHandler(statusManager::setStatusMessage);
        billsManager.setWarningMessageHandler(warningManager::setWarning);
        billsManager.setOnBillsUpdatedCallback(this::refreshAfterBillsUpdated);
        billsManager.setFilterSetterCallback((year, month) -> {
            filterController.setFilterValues(year, month, null);
            onApplyFilter();
        });

    }

    private void refreshAfterWorkLogged() {
        System.out.println("🔍 DEBUG: refreshAfterWorkLogged called");

        // Get the date of the log entry that was just added
        LocalDate entryDate = workLogEntryController.getLastAddedEntryDate();
        String company = workLogEntryController.getLastAddedCompany();

        // IMPORTANT: Reload all data first to ensure we have the latest entries
        try {
            service.reloadRegistros();
        } catch (Exception e) {
            System.err.println("Error reloading registros: " + e.getMessage());
            e.printStackTrace();
        }

        if (entryDate != null) {
            System.out.println("🔍 DEBUG: Last added entry date: " + entryDate);

            // Refresh the year/month map AFTER reload
            filterController.refreshYearToMonthsMap();

            // Update filter dropdowns and set to match entry date
            String year = String.valueOf(entryDate.getYear());
            String month = String.format("%02d", entryDate.getMonthValue());

            System.out.println("🔍 DEBUG: Setting filters to: " + year + "-" + month);

            // Update year dropdown items before setting values
            filterController.updateYearFilterItems();

            // Now set the filter values
            filterController.setFilterValues(year, month, "All");

            // Apply the filter
            onApplyFilter();
        } else {
            System.out.println("🔍 DEBUG: No date available for last added entry, using fallback");
            // Fallback to old behavior if no date available - but still reload
            filterController.refreshYearToMonthsMap();
            filterController.updateYearFilterItems();
            filterController.updateMonthFilter();
            onApplyFilter();
        }
    }
    private void updateFiltersWithYearMonth(String year, String month) {
        try {
            System.out.println("🔍 DEBUG: Updating filters to: " + year + "-" + month);

            // Reload data first to ensure we have the latest changes
            service.reloadRegistros();

            // Refresh filter data
            filterController.refreshYearToMonthsMap();

            // Update year dropdown items (this adds the new year if it wasn't there before)
            filterController.updateYearFilterItems();

            // Set the filter values to the new date
            filterController.setFilterValues(year, month, "All");

            // Apply the filter
            onApplyFilter();

        } catch (Exception e) {
            System.err.println("Error updating filters: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void refreshAfterBillsUpdated() {
        try {
            service.clearBillCache();
            filterController.refreshYearToMonthsMap();
            filterController.updateYearFilterItems();

            // Use the specific edited year/month if available
            String yearToUse = billsManager.getLastEditedYear();
            String monthToUse = billsManager.getLastEditedMonth();

            if (yearToUse != null && monthToUse != null) {
                System.out.println("Setting filters to edited bill date: " + yearToUse + "-" + monthToUse);
                filterController.setFilterValues(yearToUse, monthToUse, null);
            } else {
                filterController.updateMonthFilter();
            }

            onApplyFilter();
            Platform.runLater(logTableController::scrollToMostRecentBill);
        } catch (Exception e) {
            System.err.println("Error refreshing after bills update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set filters to specific year and month
     */
    public void setFiltersToDate(String year, String month, String company) {
        filterController.refreshYearToMonthsMap();
        filterController.updateYearFilterItems();
        filterController.setFilterValues(year, month, company);
        onApplyFilter();
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

    @FXML
    public void onOpenLogEditor() {
        LogEditorUI editor = new LogEditorUI();

        // Create a filter callback that first reloads the data, then sets the filter
        editor.setOnFilterCallback((year, month) -> {
            updateFiltersWithYearMonth(year, month);
        });

        // The regular close callback (for when filter callback isn't triggered)
        editor.setOnClose(() -> {
            try {
                service.reloadRegistros();

                // Only update filters if no specific filter callback was triggered
                // Find most recently edited log
                List<RegistroTrabalho> logs = service.getRegistros();
                if (!logs.isEmpty()) {
                    logs.sort((a, b) -> {
                        try {
                            LocalDate dateA = LocalDate.parse(a.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                            LocalDate dateB = LocalDate.parse(b.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                            return dateB.compareTo(dateA); // Recent first
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                    RegistroTrabalho latest = logs.get(0);
                    LocalDate editedDate = LocalDate.parse(latest.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                    // Set filters directly instead of using updateMonthFilter which resets to All
                    filterController.refreshYearToMonthsMap();
                    filterController.updateYearFilterItems();
                    // Critical - set values directly
                    String year = String.valueOf(editedDate.getYear());
                    String month = String.format("%02d", editedDate.getMonthValue());
                    System.out.println("Setting log filters to: " + year + "-" + month);
                    filterController.setFilterValues(year, month, "All");
                } else {
                    filterController.refreshYearToMonthsMap();
                    filterController.updateYearFilterItems();
                }
                onApplyFilter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Get current filters and pass to show method
        String year = filterController.getSelectedYear();
        String month = filterController.getSelectedMonth();
        String company = filterController.getSelectedCompany();

        editor.show((Stage) openLogEditorBtn.getScene().getWindow(), year, month, company);
    }}