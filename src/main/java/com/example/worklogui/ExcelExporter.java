package com.example.worklogui;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.scene.control.Alert;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExcelExporter {

    public static void exportToExcel(List<DisplayEntry> entries, CompanyManagerService service, boolean isAllExport) throws IOException {
        try {
            System.out.println("Starting Excel export...");
            Path exportPath = prepareExportPath(isAllExport);
            
            entries.sort(Comparator.comparing(DisplayEntry::getDate));
            ExportData exportData = prepareExportData(entries);
            AGICalculator.AGIResult agiResult = AGICalculator.calculateAGI(exportData.workRecords, exportData.bills);

            try (Workbook workbook = new XSSFWorkbook()) {
                System.out.println("Creating Excel workbook...");
                Sheet sheet = workbook.createSheet("WorkLog Export");
                
                ExcelStyles styles = createExcelStyles(workbook);
                setupSheetStructure(sheet, styles);
                addDataRows(sheet, entries, service, styles);
                addSummaryRows(sheet, agiResult, exportData.bills, styles, entries.size() + 2);
                
                writeWorkbookToFile(workbook, exportPath);
            }
            
            verifyFileCreation(exportPath);
            System.out.println("✅ Excel file exported successfully to: " + exportPath.toAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Excel export failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Excel export failed: " + e.getMessage(), e);
        }
    }
    
    private static Path prepareExportPath(boolean isAllExport) throws IOException {
        System.out.println("Export folder path: " + AppConstants.EXPORT_FOLDER.toAbsolutePath());
        
        if (!Files.exists(AppConstants.EXPORT_FOLDER)) {
            System.out.println("Export folder doesn't exist. Creating...");
            try {
                Files.createDirectories(AppConstants.EXPORT_FOLDER);
                System.out.println("Export folder created successfully");
            } catch (IOException e) {
                System.err.println("Failed to create export folder: " + e.getMessage());
                throw new IOException("Failed to create export directory: " + e.getMessage());
            }
        } else {
            System.out.println("Export folder already exists");
        }

        if (!Files.isWritable(AppConstants.EXPORT_FOLDER)) {
            throw new IOException("Export directory is not writable: " + AppConstants.EXPORT_FOLDER);
        }

        String filePrefix = isAllExport ? "all_summary_with_bills" : "summary_with_bills";
        String timestamp = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").format(LocalDateTime.now());
        Path exportPath = AppConstants.EXPORT_FOLDER.resolve(filePrefix + "_" + timestamp + ".xlsx");
        
        System.out.println("Export file path: " + exportPath.toAbsolutePath());
        return exportPath;
    }
    
    private static ExportData prepareExportData(List<DisplayEntry> entries) {
        List<RegistroTrabalho> workRecords = new ArrayList<>();
        List<Bill> bills = new ArrayList<>();

        for (DisplayEntry entry : entries) {
            if (entry.isBill()) {
                bills.add(entry.getBill());
            } else {
                workRecords.add(entry.getRegistro());
            }
        }
        
        return new ExportData(workRecords, bills);
    }
    
    private static ExcelStyles createExcelStyles(Workbook workbook) {
        return new ExcelStyles(workbook);
    }
    
    private static void setupSheetStructure(Sheet sheet, ExcelStyles styles) {
        // Set column widths
        sheet.setColumnWidth(0, 12 * 256); // Type
        sheet.setColumnWidth(1, 20 * 256); // Category
        sheet.setColumnWidth(2, 12 * 256); // Date
        sheet.setColumnWidth(3, 10 * 256); // Hours
        sheet.setColumnWidth(4, 10 * 256); // Minutes
        sheet.setColumnWidth(5, 30 * 256); // Total Hours
        sheet.setColumnWidth(6, 15 * 256); // Earnings
        sheet.setColumnWidth(7, 12 * 256); // Deductible
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"TYPE", "CATEGORY", "DATE", "HOURS", "MINUTES", "TOTAL HOURS", "EARNINGS", "DEDUCTIBLE"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.headerStyle);
        }
    }
    
    private static void addDataRows(Sheet sheet, List<DisplayEntry> entries, CompanyManagerService service, ExcelStyles styles) {
        System.out.println("Adding " + entries.size() + " entries to Excel...");
        
        int rowNum = 1;
        double grossTotal = 0.0;
        
        for (DisplayEntry entry : entries) {
            Row row = sheet.createRow(rowNum++);
            
            if (entry.isBill()) {
                addBillRow(row, entry.getBill(), styles);
            } else {
                double earnings = service.calculateEarnings(entry.getRegistro());
                grossTotal += earnings;
                addWorkRow(row, entry.getRegistro(), earnings, styles);
            }
        }
    }
    
    private static void addBillRow(Row row, Bill bill, ExcelStyles styles) {
        row.createCell(0).setCellValue("Bill");
        row.createCell(1).setCellValue(bill.getLabel());
        
        Cell dateCell = row.createCell(2);
        dateCell.setCellValue(java.sql.Date.valueOf(bill.getDate()));
        dateCell.setCellStyle(styles.dateStyle);
        
        // Hours, Minutes, Total Hours left blank
        row.createCell(3).setCellValue("");
        row.createCell(4).setCellValue("");
        row.createCell(5).setCellValue("");
        
        Cell amountCell = row.createCell(6);
        amountCell.setCellValue(-bill.getAmount());
        amountCell.setCellStyle(styles.negativeCurrencyStyle);
        
        row.createCell(7).setCellValue(bill.isDeductible() ? "Yes" : "No");
    }
    
    private static void addWorkRow(Row row, RegistroTrabalho r, double earnings, ExcelStyles styles) {
        double totalHours = r.getHoras() + r.getMinutos() / 60.0;
        
        row.createCell(0).setCellValue("Work");
        row.createCell(1).setCellValue(r.getEmpresa());
        
        Cell dateCell = row.createCell(2);
        LocalDate date = LocalDate.parse(r.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        dateCell.setCellValue(java.sql.Date.valueOf(date));
        dateCell.setCellStyle(styles.dateStyle);
        
        Cell hoursCell = row.createCell(3);
        hoursCell.setCellValue(r.getHoras());
        hoursCell.setCellStyle(styles.numberStyle);
        
        row.createCell(4).setCellValue(r.getMinutos());
        
        Cell totalHoursCell = row.createCell(5);
        totalHoursCell.setCellValue(totalHours);
        totalHoursCell.setCellStyle(styles.numberStyle);
        
        Cell earningsCell = row.createCell(6);
        earningsCell.setCellValue(earnings);
        earningsCell.setCellStyle(styles.currencyStyle);
        
        row.createCell(7).setCellValue("N/A");
    }
    
    private static void addSummaryRows(Sheet sheet, AGICalculator.AGIResult agiResult, List<Bill> bills, ExcelStyles styles, int startRow) {
        addSummaryRow(sheet, startRow, "GROSS TOTAL", agiResult.grossIncome, styles.totalStyle, styles.currencyStyle);
        addSummaryRow(sheet, startRow + 1, "BUSINESS EXPENSES", -agiResult.businessExpenses, styles.totalStyle, styles.negativeCurrencyStyle);
        addSummaryRow(sheet, startRow + 2, "NET EARNINGS", agiResult.netEarnings, styles.totalStyle, styles.netTotalStyle);
        addSummaryRow(sheet, startRow + 3, "NESE (NET × 0.9235)", agiResult.nese, styles.totalStyle, styles.currencyStyle);
        addSummaryRow(sheet, startRow + 4, "SE TAX (15.3%)", agiResult.selfEmploymentTax, styles.totalStyle, styles.currencyStyle);
        addSummaryRow(sheet, startRow + 5, "SE TAX DEDUCTION (50%)", agiResult.selfEmploymentTaxDeduction, styles.totalStyle, styles.currencyStyle);
        addSummaryRow(sheet, startRow + 6, "ADJUSTED GROSS INCOME", agiResult.adjustedGrossIncome, styles.totalStyle, styles.agiStyle);
        addSummaryRow(sheet, startRow + 7, "MONTHLY SSA COUNTABLE", agiResult.monthlySSACountableIncome, styles.totalStyle, styles.currencyStyle);
        
        double nonDeductibleExpenses = bills.stream().filter(bill -> !bill.isDeductible()).mapToDouble(Bill::getAmount).sum();
        addSummaryRow(sheet, startRow + 8, "NON-DEDUCTIBLE EXPENSES", -nonDeductibleExpenses, styles.totalStyle, styles.negativeCurrencyStyle);
        
        double totalBillAmount = bills.stream().mapToDouble(Bill::getAmount).sum();
        double incomeAfterAllBills = agiResult.grossIncome - totalBillAmount;
        addSummaryRow(sheet, startRow + 9, "AFTER ALL BILLS", incomeAfterAllBills, styles.totalStyle, styles.cashFlowStyle);
    }
    
    private static void addSummaryRow(Sheet sheet, int rowIndex, String label, double value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex);
        
        Cell labelCell = row.createCell(5);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        
        Cell valueCell = row.createCell(6);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }
    
    private static void writeWorkbookToFile(Workbook workbook, Path exportPath) throws IOException {
        System.out.println("Writing Excel file to disk...");
        try (FileOutputStream fileOut = new FileOutputStream(exportPath.toFile())) {
            workbook.write(fileOut);
            fileOut.flush();
            System.out.println("Excel file written successfully");
        } catch (IOException e) {
            System.err.println("Failed to write Excel file: " + e.getMessage());
            throw new IOException("Failed to write Excel file: " + e.getMessage());
        }
    }
    
    private static void verifyFileCreation(Path exportPath) throws IOException {
        if (!Files.exists(exportPath)) {
            throw new IOException("Excel file was not created successfully");
        }
    }
    
    private static class ExportData {
        final List<RegistroTrabalho> workRecords;
        final List<Bill> bills;
        
        ExportData(List<RegistroTrabalho> workRecords, List<Bill> bills) {
            this.workRecords = workRecords;
            this.bills = bills;
        }
    }
    
    private static class ExcelStyles {
        final CellStyle headerStyle;
        final CellStyle currencyStyle;
        final CellStyle negativeCurrencyStyle;
        final CellStyle numberStyle;
        final CellStyle dateStyle;
        final CellStyle totalStyle;
        final CellStyle netTotalStyle;
        final CellStyle agiStyle;
        final CellStyle cashFlowStyle;
        
        ExcelStyles(Workbook workbook) {
            DataFormat dataFormat = workbook.createDataFormat();
            
            // Header style
            this.headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            
            // Currency styles
            this.currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            
            this.negativeCurrencyStyle = workbook.createCellStyle();
            negativeCurrencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            negativeCurrencyStyle.setFont(redFont);
            
            // Number and date styles
            this.numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("0.00"));
            
            this.dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("mm/dd/yyyy"));
            
            // Total styles
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            
            this.totalStyle = workbook.createCellStyle();
            totalStyle.setFont(totalFont);
            totalStyle.setBorderTop(BorderStyle.THIN);
            
            this.netTotalStyle = workbook.createCellStyle();
            netTotalStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            netTotalStyle.setFont(totalFont);
            netTotalStyle.setBorderTop(BorderStyle.DOUBLE);
            
            this.agiStyle = workbook.createCellStyle();
            agiStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            agiStyle.setFont(totalFont);
            agiStyle.setBorderTop(BorderStyle.DOUBLE);
            agiStyle.setBorderBottom(BorderStyle.DOUBLE);
            
            this.cashFlowStyle = workbook.createCellStyle();
            cashFlowStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            cashFlowStyle.setFont(totalFont);
            cashFlowStyle.setBorderTop(BorderStyle.DOUBLE);
            cashFlowStyle.setBorderBottom(BorderStyle.DOUBLE);
        }
    }
}