package com.example.worklogui;

import java.io.IOException;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompanyManagerService {

    private static final Path WORKLOG_PATH = FileLoader.getDefaultPath();
    private static final Path BILLS_DIR = Paths.get(System.getProperty("user.home"), "Documents", "WorkLog", "bills");
    // CRITICAL: Changed to always use new list instances for values to avoid shared references
    private Map<String, List<Bill>> bills = new HashMap<>();

    private List<RegistroTrabalho> registros = new ArrayList<>();

    private Set<String> years = new TreeSet<>();
    private Set<String> months = new TreeSet<>();
    private Set<String> companies = new TreeSet<>();

    public void initialize() throws Exception {
        FileLoader.inicializarArquivo(WORKLOG_PATH);
        registros = FileLoader.carregarRegistros(WORKLOG_PATH);
        CompanyRateService.getInstance().refreshRates();
        populateFilters();
    }

    public void reloadRegistros() throws Exception {
        registros = FileLoader.carregarRegistros(WORKLOG_PATH);
        populateFilters();
    }

    public List<RegistroTrabalho> getRegistros() {
        return registros;
    }

    public RegistroTrabalho logWork(LocalDate date, String company, double timeValue, boolean doublePay) throws Exception {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

        RateInfo info = CompanyRateService.getInstance().getRateInfoMap().getOrDefault(company, new RateInfo(0.0, "hour"));

        RegistroTrabalho newEntry = new RegistroTrabalho();
        newEntry.setData(formattedDate);
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

        registros.add(newEntry);
        FileLoader.salvarRegistros(WORKLOG_PATH, registros);
        populateFilters();

        return newEntry;
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
            // Store a defensive copy
            this.bills.put(yearMonth, new ArrayList<>(result));
            System.out.println("🔍 DEBUG: Loaded " + result.size() + " bills");
        }
        // Return a defensive copy
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
                // Remove from cache when file is deleted
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
                    // Update cache with a defensive copy
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

        // Verify file state after operation
        System.out.println("Final file state - exists: " + Files.exists(path));
        if (Files.exists(path)) {
            System.out.println("File size: " + Files.size(path) + " bytes");
        }
    }

    public void clearBillCache() {
        this.bills.clear(); // Clear the cache to force reload from disk
        System.out.println("🔄 Bill cache cleared");
    }

    public Path getBillPath(String yearMonth) {
        return BILLS_DIR.resolve(yearMonth + ".json");
    }

    public List<Bill> carregarBills(Path path) {
        return FileLoader.carregarBills(path);
    }

    public void deleteRegistro(RegistroTrabalho registro) throws Exception {
        registros.remove(registro);
        FileLoader.salvarRegistros(WORKLOG_PATH, registros);

        // Reload data to ensure consistency
        registros = FileLoader.carregarRegistros(WORKLOG_PATH);

        populateFilters();
    }

    public List<RegistroTrabalho> applyFilters(String year, String month, String company) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        List<RegistroTrabalho> filtered = new ArrayList<>();

        for (RegistroTrabalho r : registros) {
            try {
                LocalDate date = LocalDate.parse(r.getData(), formatter);
                boolean match = true;

                if (year != null && !year.isEmpty()) {
                    match &= String.valueOf(date.getYear()).equals(year);
                }

                if (month != null && !month.isEmpty()) {
                    match &= String.format("%02d", date.getMonthValue()).equals(month);
                }

                // Handle "All" or null as no filter
                if (company != null && !company.equalsIgnoreCase("All") && !company.isEmpty()) {
                    match &= company.equals(r.getEmpresa());
                }

                if (match) filtered.add(r);
            } catch (Exception e) {
                System.err.println("⚠ Skipping invalid date: " + r.getData());
            }
        }

        return filtered;
    }

    public void populateFilters() {
        years.clear();
        months.clear();
        companies.clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        for (RegistroTrabalho r : registros) {
            try {
                LocalDate date = LocalDate.parse(r.getData(), formatter);
                years.add(String.valueOf(date.getYear()));
                months.add(String.format("%02d", date.getMonthValue()));
            } catch (Exception e) {
                System.err.println("⚠ Invalid date format: " + r.getData());
            }

            if (r.getEmpresa() != null) {
                companies.add(r.getEmpresa());
            }
        }
    }

    public Set<String> getYears() { return years; }
    public Set<String> getMonths() { return months; }
    public Set<String> getCompanies() { return companies; }

    public Map<String, List<Bill>> getAllBills() {
        // Always create a fresh map
        Map<String, List<Bill>> all = new HashMap<>();

        // Clear cache first to ensure we reload from disk
        this.bills.clear();

        if (Files.exists(BILLS_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(BILLS_DIR, "*.json")) {
                for (Path path : stream) {
                    String filename = path.getFileName().toString(); // e.g., "2026-12.json"
                    String ym = filename.replace(".json", "");
                    List<Bill> billList = carregarBills(path);
                    if (!billList.isEmpty()) {
                        // Update cache with a defensive copy
                        this.bills.put(ym, new ArrayList<>(billList));
                        // Add to result map
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

        for (RegistroTrabalho r : registros) {
            String empresa = r.getEmpresa();
            totais.putIfAbsent(empresa, new double[2]);
            totais.get(empresa)[0] += r.getHoras();
            totais.get(empresa)[1] += r.getMinutos();
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

        for (RegistroTrabalho r : registros) {
            String empresa = r.getEmpresa();
            double ganho = calculateEarnings(r);
            ganhos.put(empresa, ganhos.getOrDefault(empresa, 0.0) + ganho);
            total += ganho;
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
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        for (RegistroTrabalho r : registros) {
            try {
                LocalDate date = LocalDate.parse(r.getData(), formato);
                String year = String.valueOf(date.getYear());
                String monthKey = year + "-" + String.format("%02d", date.getMonthValue());
                double earnings = calculateEarnings(r);

                monthTotals.put(monthKey, monthTotals.getOrDefault(monthKey, 0.0) + earnings);
                yearTotals.put(year, yearTotals.getOrDefault(year, 0.0) + earnings);
            } catch (Exception ignored) {}
        }

        StringBuilder sb = new StringBuilder("📆 Year-Month Summary:\n\n");
        String currentYear = "";
        for (String monthKey : monthTotals.keySet()) {
            String year = monthKey.substring(0, 4);
            if (!year.equals(currentYear)) {
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
        List<DisplayEntry> allEntries = new ArrayList<>();
        for (RegistroTrabalho r : registros) {
            allEntries.add(new DisplayEntry(r));
        }

        // Clear bill cache to ensure fresh data
        clearBillCache();

        for (List<Bill> monthlyBills : getAllBills().values()) {
            allEntries.addAll(monthlyBills.stream().map(DisplayEntry::new).toList());
        }
        allEntries.sort(Comparator.comparing(DisplayEntry::getDate));
        boolean isAllExport = true;
        ExcelExporter.exportToExcel(allEntries, this, isAllExport);
    }
}



