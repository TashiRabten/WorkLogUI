package com.example.worklogui;

import com.example.worklogui.services.WorkLogFileManager;
import com.example.worklogui.utils.DateUtils;
import com.example.worklogui.utils.ErrorHandler;
import com.example.worklogui.utils.FilterHelper;
import com.example.worklogui.utils.ValidationHelper;
import com.example.worklogui.utils.FileMigrationUtility;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class CompanyManagerService {

    private static final Path BILLS_DIR = Paths.get(System.getProperty("user.home"), "Documents", "WorkLog", "bills");

    // Use the new file manager for work logs
    private final WorkLogFileManager workLogFileManager = new WorkLogFileManager();

    // Cache for bills (keep existing pattern)
    private Map<String, List<Bill>> bills = new HashMap<>();

    // Cached filter data
    private Set<String> years = new TreeSet<>();
    private Set<String> months = new TreeSet<>();
    private Set<String> companies = new TreeSet<>();

    public void initialize() throws Exception {
        try {
            // Initialize the work log file manager
            workLogFileManager.initialize();

            // Check for and migrate old worklog.json file if it exists
            migrateOldWorklogFileIfExists();

            // Initialize company rates
            CompanyRateService.getInstance().refreshRates();

            // Populate filters from available data
            populateFilters();

            // Set up error handler
            ErrorHandler.setStatusMessageHandler(message -> {
                System.err.println("Service Error: " + message);
            });

        } catch (Exception e) {
            throw new Exception("Failed to initialize service: " + e.getMessage(), e);
        }
    }

    /**
     * Check for old worklog.json file and migrate it if it exists
     */
    private void migrateOldWorklogFileIfExists() {
        try {
            Path oldWorklogFile = AppConstants.WORKLOG_PATH;
            Path logsDirectory = AppConstants.LOGS_FOLDER;

            if (Files.exists(oldWorklogFile)) {
                System.out.println("🔍 Found old worklog.json file, starting migration...");
                FileMigrationUtility.migrateOldWorklogFile(oldWorklogFile, logsDirectory);
                System.out.println("✅ Migration completed successfully");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Migration failed, but continuing with initialization: " + e.getMessage());
            // Don't throw exception here - allow app to continue even if migration fails
        }
    }

    public void reloadRegistros() throws Exception {
        try {
            // Clear cache to force reload
            workLogFileManager.clearCache();
            populateFilters();
        } catch (Exception e) {
            throw new Exception("Failed to reload work logs: " + e.getMessage(), e);
        }
    }

    public List<RegistroTrabalho> getRegistros() {
        try {
            return workLogFileManager.getAllWorkLogs();
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("loading all work logs", e);
            return new ArrayList<>();
        }
    }

    public RegistroTrabalho logWork(LocalDate date, String company, double timeValue, boolean doublePay) throws Exception {
        // Validate inputs
        String dateString = DateUtils.formatDisplayDate(date);
        ValidationHelper.WorkLogValidationResult validation =
                ValidationHelper.validateWorkLogEntry(dateString, company, String.valueOf(timeValue));

        if (!validation.isValid()) {
            throw new Exception(validation.getErrorMessage());
        }

        // Get rate info for company
        RateInfo info = CompanyRateService.getInstance().getRateInfoMap().getOrDefault(company, new RateInfo(0.0, "hour"));

        // Create new work log entry
        RegistroTrabalho newEntry = new RegistroTrabalho();
        newEntry.setData(dateString);
        newEntry.setEmpresa(company);
        newEntry.setPagamentoDobrado(doublePay);
        newEntry.setTaxaUsada(info.getValor());
        newEntry.setTipoUsado(info.getTipo());

        if (info.getTipo().equalsIgnoreCase("minuto")) {
            newEntry.setHoras(0);
            newEntry.setMinutos(timeValue);
        } else {
            newEntry.setHoras(timeValue);
            newEntry.setMinutos(0);
        }

        try {
            // Save using the file manager
            workLogFileManager.addWorkLog(newEntry);

            // Update filters
            populateFilters();

            return newEntry;
        } catch (Exception e) {
            throw new Exception("Failed to save work log: " + e.getMessage(), e);
        }
    }

    public double calculateEarnings(RegistroTrabalho registro) {
        double taxa = registro.getTaxaUsada();
        String tipo = registro.getTipoUsado();
        if (tipo == null) tipo = "hour";

        double ganho = tipo.equalsIgnoreCase("minuto") ? registro.getMinutos() * taxa : registro.getHoras() * taxa;
        return registro.isPagamentoDobrado() ? ganho * 2 : ganho;
    }

    public double calculateTotalBills(String yearMonth) {
        Path path = getBillPath(yearMonth);
        List<Bill> bills = carregarBills(path);
        return bills.stream().mapToDouble(Bill::getAmount).sum();
    }

    public List<Bill> getBillsForMonth(String yearMonth) {
        if (!this.bills.containsKey(yearMonth)) {
            Path path = getBillPath(yearMonth);
            System.out.println("🔍 DEBUG: Loading bills from: " + path);
            List<Bill> result = carregarBills(path);
            this.bills.put(yearMonth, new ArrayList<>(result));
            System.out.println("🔍 DEBUG: Loaded " + result.size() + " bills");
        }
        return new ArrayList<>(this.bills.getOrDefault(yearMonth, new ArrayList<>()));
    }

    public void setBillsForMonth(String yearMonth, List<Bill> billList) throws Exception {
        Path path = getBillPath(yearMonth);
        Files.createDirectories(path.getParent());

        if (billList.isEmpty()) {
            try {
                if (Files.exists(path)) {
                    System.out.println("Deleting empty bill file: " + path);
                    Files.delete(path);
                }
                this.bills.remove(yearMonth);
                System.out.println("Removed " + yearMonth + " from bills cache");
            } catch (IOException e) {
                System.out.println("❌ ERROR: Could not delete bill file " + path.getFileName());
                e.printStackTrace();
                throw e;
            }
        } else {
            try {
                System.out.println("Saving " + billList.size() + " bills to " + yearMonth);
                boolean success = FileLoader.salvarBills(path, billList);
                if (success) {
                    this.bills.put(yearMonth, new ArrayList<>(billList));
                    System.out.println("💾 Saved " + billList.size() + " bills to file.");
                } else {
                    System.out.println("❌ ERROR: Failed to save bills to file");
                    throw new IOException("Failed to save bills to " + path.getFileName());
                }
            } catch (IOException e) {
                System.out.println("❌ ERROR: Could not save bills file " + path.getFileName());
                e.printStackTrace();
                throw e;
            }
        }

        System.out.println("Final file state - exists: " + Files.exists(path));
        if (Files.exists(path)) {
            System.out.println("File size: " + Files.size(path) + " bytes");
        }
    }

    public void clearBillCache() {
        this.bills.clear();
        System.out.println("🔄 Bill cache cleared");
    }

    public Path getBillPath(String yearMonth) {
        return BILLS_DIR.resolve(yearMonth + ".json");
    }

    public List<Bill> carregarBills(Path path) {
        return FileLoader.carregarBills(path);
    }

    public void deleteRegistro(RegistroTrabalho registro) throws Exception {
        try {
            boolean removed = workLogFileManager.removeWorkLog(registro);
            if (!removed) {
                throw new Exception("Work log entry not found for deletion");
            }

            // Update filters
            populateFilters();
        } catch (Exception e) {
            throw new Exception("Failed to delete work log: " + e.getMessage(), e);
        }
    }

    public List<RegistroTrabalho> applyFilters(String year, String month, String company) {
        try {
            return workLogFileManager.getFilteredWorkLogs(year, month, company);
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("filtering work logs", e);
            return new ArrayList<>();
        }
    }

    public void populateFilters() {
        try {
            List<RegistroTrabalho> allLogs = workLogFileManager.getAllWorkLogs();

            years.clear();
            months.clear();
            companies.clear();

            years.addAll(FilterHelper.extractYears(allLogs));
            months.addAll(FilterHelper.extractMonths(allLogs));
            companies.addAll(FilterHelper.extractCompanies(allLogs));

            // Also add years/months from bills
            Map<String, List<Bill>> allBills = getAllBills();
            for (String yearMonth : allBills.keySet()) {
                if (yearMonth.length() >= 7) {
                    String year = DateUtils.getYearFromKey(yearMonth);
                    String month = DateUtils.getMonthFromKey(yearMonth);
                    if (year != null && month != null) {
                        years.add(year);
                        months.add(month);
                    }
                }
            }
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("populating filters", e);
        }
    }

    public Set<String> getYears() { return years; }
    public Set<String> getMonths() { return months; }
    public Set<String> getCompanies() { return companies; }

    public Map<String, List<Bill>> getAllBills() {
        Map<String, List<Bill>> all = new HashMap<>();
        this.bills.clear();

        if (Files.exists(BILLS_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(BILLS_DIR, "*.json")) {
                for (Path path : stream) {
                    String filename = path.getFileName().toString();
                    String ym = filename.replace(".json", "");
                    List<Bill> billList = carregarBills(path);
                    if (!billList.isEmpty()) {
                        this.bills.put(ym, new ArrayList<>(billList));
                        all.put(ym, new ArrayList<>(billList));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return all;
    }

    public String calculateTimeTotal() {
        Map<String, double[]> totais = new HashMap<>();

        try {
            List<RegistroTrabalho> allLogs = workLogFileManager.getAllWorkLogs();
            for (RegistroTrabalho r : allLogs) {
                String empresa = r.getEmpresa();
                totais.putIfAbsent(empresa, new double[2]);
                totais.get(empresa)[0] += r.getHoras();
                totais.get(empresa)[1] += r.getMinutos();
            }
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("calculating time total", e);
            return "Error calculating time total: " + e.getMessage();
        }

        StringBuilder sb = new StringBuilder("Total Summary / Resumo Total:\n\n");

        for (Map.Entry<String, double[]> e : totais.entrySet()) {
            String empresa = e.getKey();
            double horas = e.getValue()[0];
            double minutos = e.getValue()[1];

            if (minutos >= 60) {
                int horasFromMinutos = (int) (minutos / 60);
                horas += horasFromMinutos;
                minutos = minutos % 60;
            }

            double totalHoras = horas + (minutos / 60.0);
            sb.append(String.format("%s: %.2f hours, %.2f minutes\n", empresa, horas, minutos));
            sb.append(String.format(" → Total in hours: %.2f\n\n", totalHoras));
        }

        return sb.toString();
    }

    public String calculateEarnings() {
        double total = 0;
        Map<String, Double> ganhos = new HashMap<>();

        try {
            List<RegistroTrabalho> allLogs = workLogFileManager.getAllWorkLogs();
            for (RegistroTrabalho r : allLogs) {
                String empresa = r.getEmpresa();
                double ganho = calculateEarnings(r);
                ganhos.put(empresa, ganhos.getOrDefault(empresa, 0.0) + ganho);
                total += ganho;
            }
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("calculating earnings", e);
            return "Error calculating earnings: " + e.getMessage();
        }

        StringBuilder sb = new StringBuilder("Earnings / Ganho Total:\n\n");
        for (Map.Entry<String, Double> e : ganhos.entrySet()) {
            sb.append(String.format("%s: $%.2f\n", e.getKey(), e.getValue()));
        }
        sb.append(String.format("\nGrand Total / Total Geral: $%.2f", total));

        return sb.toString();
    }

    public String getSummaryByMonthAndYear() {
        Map<String, Double> monthTotals = new TreeMap<>();
        Map<String, Double> yearTotals = new TreeMap<>();

        try {
            List<RegistroTrabalho> allLogs = workLogFileManager.getAllWorkLogs();
            for (RegistroTrabalho r : allLogs) {
                try {
                    LocalDate date = DateUtils.parseDisplayDate(r.getData());
                    String year = String.valueOf(date.getYear());
                    String monthKey = DateUtils.getYearMonthKey(date);
                    double earnings = calculateEarnings(r);

                    monthTotals.put(monthKey, monthTotals.getOrDefault(monthKey, 0.0) + earnings);
                    yearTotals.put(year, yearTotals.getOrDefault(year, 0.0) + earnings);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("calculating summary", e);
            return "Error calculating summary: " + e.getMessage();
        }

        StringBuilder sb = new StringBuilder("📆 Year-Month Summary:\n\n");
        String currentYear = "";
        for (String monthKey : monthTotals.keySet()) {
            String year = DateUtils.getYearFromKey(monthKey);
            if (year != null && !year.equals(currentYear)) {
                if (!currentYear.isEmpty()) {
                    // Insert the total for the previous year
                    sb.append(String.format("%s Grand Total → $%.2f\n\n", currentYear, yearTotals.get(currentYear)));
                }
                currentYear = year;
                sb.append("📅 ").append(year).append("\n-------------------\n");
            }
            sb.append(String.format("%s → $%.2f\n-----------------------\n", monthKey, monthTotals.get(monthKey)));
        }

        // Add final year total
        if (!currentYear.isEmpty()) {
            sb.append(String.format("%s Grand Total → $%.2f\n", currentYear, yearTotals.get(currentYear)));
        }

        return sb.toString();
    }

    public void exportToExcel() throws IOException {
        try {
            List<DisplayEntry> allEntries = new ArrayList<>();

            // Add all work logs
            List<RegistroTrabalho> allLogs = workLogFileManager.getAllWorkLogs();
            for (RegistroTrabalho r : allLogs) {
                allEntries.add(new DisplayEntry(r));
            }

            // Clear bill cache to ensure fresh data
            clearBillCache();

            // Add all bills
            for (List<Bill> monthlyBills : getAllBills().values()) {
                allEntries.addAll(monthlyBills.stream().map(DisplayEntry::new).toList());
            }

            allEntries.sort(Comparator.comparing(DisplayEntry::getDate));
            boolean isAllExport = true;
            ExcelExporter.exportToExcel(allEntries, this, isAllExport);

        } catch (Exception e) {
            throw new IOException("Failed to export to Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing work log entry
     */
    public void updateWorkLog(RegistroTrabalho oldLog, RegistroTrabalho newLog) throws Exception {
        try {
            boolean updated = workLogFileManager.updateWorkLog(oldLog, newLog);
            if (!updated) {
                throw new Exception("Work log entry not found for update");
            }

            // Update filters
            populateFilters();
        } catch (Exception e) {
            throw new Exception("Failed to update work log: " + e.getMessage(), e);
        }
    }

    /**
     * Get work logs for a specific year-month
     */
    public List<RegistroTrabalho> getWorkLogsForMonth(String yearMonthKey) {
        try {
            return workLogFileManager.getWorkLogs(yearMonthKey);
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("loading work logs for " + yearMonthKey, e);
            return new ArrayList<>();
        }
    }

    /**
     * Clear work log cache
     */
    public void clearWorkLogCache() {
        workLogFileManager.clearCache();
    }

    /**
     * Clear cache for specific month
     */
    public void clearWorkLogCache(String yearMonthKey) {
        workLogFileManager.clearCache(yearMonthKey);
    }

    /**
     * Get the work log file manager (for advanced operations)
     */
    public WorkLogFileManager getWorkLogFileManager() {
        return workLogFileManager;
    }

    /**
     * Save work logs for a specific month (used by external components)
     */
    public void saveWorkLogsForMonth(String yearMonthKey, List<RegistroTrabalho> logs) throws Exception {
        try {
            workLogFileManager.saveWorkLogs(yearMonthKey, logs);
        } catch (Exception e) {
            throw new Exception("Failed to save work logs for " + yearMonthKey + ": " + e.getMessage(), e);
        }
    }

    /**
     * Cleanup old backup files
     */
    public void cleanupOldBackups() {
        workLogFileManager.cleanupOldBackups();
    }

    /**
     * Get year-to-months mapping from work logs
     */
    public Map<String, List<String>> getYearToMonthsMap() {
        try {
            List<RegistroTrabalho> allLogs = workLogFileManager.getAllWorkLogs();
            return FilterHelper.buildYearToMonthsMap(allLogs);
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("building year-month map", e);
            return new HashMap<>();
        }
    }
}