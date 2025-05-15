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
            // Debug output
            System.out.println("Starting Excel export...");
            System.out.println("Export folder path: " + AppConstants.EXPORT_FOLDER.toAbsolutePath());

            // Create directory if it doesn't exist with better error handling
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

            // Check if directory is writable
            if (!Files.isWritable(AppConstants.EXPORT_FOLDER)) {
                throw new IOException("Export directory is not writable: " + AppConstants.EXPORT_FOLDER);
            }

            // Generate filename with a descriptive name and formatted timestamp
            String filePrefix = isAllExport ? "all_summary_with_bills" : "summary_with_bills";
            String timestamp = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").format(LocalDateTime.now());
            Path exportPath = AppConstants.EXPORT_FOLDER.resolve(filePrefix + "_" + timestamp + ".xlsx");

            System.out.println("Export file path: " + exportPath.toAbsolutePath());

            // Sort entries by date before exporting
            entries.sort(Comparator.comparing(DisplayEntry::getDate));

            // Track totals
            double grossTotal = 0.0;

            // Create list of work records and bills for AGI calculation
            List<RegistroTrabalho> workRecords = new ArrayList<>();
            List<Bill> bills = new ArrayList<>();

            for (DisplayEntry entry : entries) {
                if (entry.isBill()) {
                    bills.add(entry.getBill());
                } else {
                    workRecords.add(entry.getRegistro());
                }
            }

            // Calculate AGI using the same logic as in the app
            AGICalculator.AGIResult agiResult = AGICalculator.calculateAGI(workRecords, bills);

            // Create workbook and sheet
            try (Workbook workbook = new XSSFWorkbook()) {
                System.out.println("Creating Excel workbook...");
                Sheet sheet = workbook.createSheet("WorkLog Export");

                // Set column widths - Increased "Total Hours" column width to fit label text
                sheet.setColumnWidth(0, 12 * 256); // Type
                sheet.setColumnWidth(1, 20 * 256); // Category
                sheet.setColumnWidth(2, 12 * 256); // Date
                sheet.setColumnWidth(3, 10 * 256); // Hours
                sheet.setColumnWidth(4, 10 * 256); // Minutes
                sheet.setColumnWidth(5, 30 * 256); // Total Hours (increased width)
                sheet.setColumnWidth(6, 15 * 256); // Earnings
                sheet.setColumnWidth(7, 12 * 256); // Deductible

                // Create styles
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(BorderStyle.THIN);

                // Currency style with 2 decimals to match app display
                CellStyle currencyStyle = workbook.createCellStyle();
                DataFormat dataFormat = workbook.createDataFormat();
                currencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));

                // Negative currency style with 2 decimals
                CellStyle negativeCurrencyStyle = workbook.createCellStyle();
                negativeCurrencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
                Font redFont = workbook.createFont();
                redFont.setColor(IndexedColors.RED.getIndex());
                negativeCurrencyStyle.setFont(redFont);

                // Number style with 2 decimals to match app display
                CellStyle numberStyle = workbook.createCellStyle();
                numberStyle.setDataFormat(dataFormat.getFormat("0.00"));

                CellStyle dateStyle = workbook.createCellStyle();
                dateStyle.setDataFormat(dataFormat.getFormat("mm/dd/yyyy"));

                CellStyle totalStyle = workbook.createCellStyle();
                Font totalFont = workbook.createFont();
                totalFont.setBold(true);
                totalStyle.setFont(totalFont);
                totalStyle.setBorderTop(BorderStyle.THIN);

                // Create header row
                Row headerRow = sheet.createRow(0);
                String[] headers = {"TYPE", "CATEGORY", "DATE", "HOURS", "MINUTES", "TOTAL HOURS", "EARNINGS", "DEDUCTIBLE"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                System.out.println("Adding " + entries.size() + " entries to Excel...");

                // Add data rows
                int rowNum = 1;
                for (DisplayEntry entry : entries) {
                    Row row = sheet.createRow(rowNum++);

                    if (entry.isBill()) {
                        Bill bill = entry.getBill();

                        // Type
                        row.createCell(0).setCellValue("Bill");

                        // Category
                        row.createCell(1).setCellValue(bill.getLabel());

                        // Date
                        Cell dateCell = row.createCell(2);
                        dateCell.setCellValue(java.sql.Date.valueOf(bill.getDate()));
                        dateCell.setCellStyle(dateStyle);

                        // Hours, Minutes, Total Hours left blank
                        row.createCell(3).setCellValue("");
                        row.createCell(4).setCellValue("");
                        row.createCell(5).setCellValue("");

                        // Earnings (negative for bills)
                        Cell amountCell = row.createCell(6);
                        amountCell.setCellValue(-bill.getAmount()); // Negative for bills
                        amountCell.setCellStyle(negativeCurrencyStyle);

                        // Deductible
                        row.createCell(7).setCellValue(bill.isDeductible() ? "Yes" : "No");
                    } else {
                        RegistroTrabalho r = entry.getRegistro();
                        double earnings = service.calculateEarnings(r);
                        grossTotal += earnings;
                        double totalHours = r.getHoras() + r.getMinutos() / 60.0;

                        // Type
                        row.createCell(0).setCellValue("Work");

                        // Category
                        row.createCell(1).setCellValue(r.getEmpresa());

                        // Date
                        Cell dateCell = row.createCell(2);
                        // Parse date from the string format
                        LocalDate date = LocalDate.parse(r.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        dateCell.setCellValue(java.sql.Date.valueOf(date));
                        dateCell.setCellStyle(dateStyle);

                        // Hours - with 2 decimal places to match app
                        Cell hoursCell = row.createCell(3);
                        hoursCell.setCellValue(r.getHoras());
                        hoursCell.setCellStyle(numberStyle);

                        // Minutes
                        row.createCell(4).setCellValue(r.getMinutos());

                        // Total Hours - with 2 decimal places to match app
                        Cell totalHoursCell = row.createCell(5);
                        totalHoursCell.setCellValue(totalHours);
                        totalHoursCell.setCellStyle(numberStyle);

                        // Earnings - with 2 decimal places to match app
                        Cell earningsCell = row.createCell(6);
                        earningsCell.setCellValue(earnings);
                        earningsCell.setCellStyle(currencyStyle);

                        // Deductible (N/A for work entries)
                        row.createCell(7).setCellValue("N/A");
                    }
                }

                // Add summary rows with data from AGIResult
                int summaryStartRow = rowNum + 1;

                // Gross Total
                Row grossRow = sheet.createRow(summaryStartRow);
                Cell grossLabelCell = grossRow.createCell(5);
                grossLabelCell.setCellValue("GROSS TOTAL");
                grossLabelCell.setCellStyle(totalStyle);

                Cell grossValueCell = grossRow.createCell(6);
                grossValueCell.setCellValue(agiResult.grossIncome);
                grossValueCell.setCellStyle(currencyStyle);

                // Business Expenses Total
                Row billRow = sheet.createRow(summaryStartRow + 1);
                Cell billLabelCell = billRow.createCell(5);
                billLabelCell.setCellValue("BUSINESS EXPENSES");
                billLabelCell.setCellStyle(totalStyle);

                Cell billValueCell = billRow.createCell(6);
                billValueCell.setCellValue(-agiResult.businessExpenses);
                billValueCell.setCellStyle(negativeCurrencyStyle);

                // Net Earnings
                Row netRow = sheet.createRow(summaryStartRow + 2);
                Cell netLabelCell = netRow.createCell(5);
                netLabelCell.setCellValue("NET EARNINGS");
                netLabelCell.setCellStyle(totalStyle);

                Cell netValueCell = netRow.createCell(6);
                netValueCell.setCellValue(agiResult.netEarnings);

                CellStyle netTotalStyle = workbook.createCellStyle();
                netTotalStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
                netTotalStyle.setFont(totalFont);
                netTotalStyle.setBorderTop(BorderStyle.DOUBLE);
                netValueCell.setCellStyle(netTotalStyle);

                // Add NESE (Net Earnings from Self-Employment)
                Row neseRow = sheet.createRow(summaryStartRow + 3);
                Cell neseLabelCell = neseRow.createCell(5);
                neseLabelCell.setCellValue("NESE (NET × 0.9235)");
                neseLabelCell.setCellStyle(totalStyle);

                Cell neseValueCell = neseRow.createCell(6);
                neseValueCell.setCellValue(agiResult.nese);
                neseValueCell.setCellStyle(currencyStyle);

                // SE Tax
                Row seTaxRow = sheet.createRow(summaryStartRow + 4);
                Cell seTaxLabelCell = seTaxRow.createCell(5);
                seTaxLabelCell.setCellValue("SE TAX (15.3%)");
                seTaxLabelCell.setCellStyle(totalStyle);

                Cell seTaxValueCell = seTaxRow.createCell(6);
                seTaxValueCell.setCellValue(agiResult.selfEmploymentTax);
                seTaxValueCell.setCellStyle(currencyStyle);

                // SE Tax Deduction
                Row seTaxDeductionRow = sheet.createRow(summaryStartRow + 5);
                Cell seTaxDeductionLabelCell = seTaxDeductionRow.createCell(5);
                seTaxDeductionLabelCell.setCellValue("SE TAX DEDUCTION (50%)");
                seTaxDeductionLabelCell.setCellStyle(totalStyle);

                Cell seTaxDeductionValueCell = seTaxDeductionRow.createCell(6);
                seTaxDeductionValueCell.setCellValue(agiResult.selfEmploymentTaxDeduction);
                seTaxDeductionValueCell.setCellStyle(currencyStyle);

                // AGI
                Row agiRow = sheet.createRow(summaryStartRow + 6);
                Cell agiLabelCell = agiRow.createCell(5);
                agiLabelCell.setCellValue("ADJUSTED GROSS INCOME");
                agiLabelCell.setCellStyle(totalStyle);

                Cell agiValueCell = agiRow.createCell(6);
                agiValueCell.setCellValue(agiResult.adjustedGrossIncome);

                CellStyle agiStyle = workbook.createCellStyle();
                agiStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
                agiStyle.setFont(totalFont);
                agiStyle.setBorderTop(BorderStyle.DOUBLE);
                agiStyle.setBorderBottom(BorderStyle.DOUBLE);
                agiValueCell.setCellStyle(agiStyle);

                // Add monthly SSA countable income (for SGA purposes)
                Row monthlySsaRow = sheet.createRow(summaryStartRow + 7);
                Cell monthlySsaLabelCell = monthlySsaRow.createCell(5);
                monthlySsaLabelCell.setCellValue("MONTHLY SSA COUNTABLE");
                monthlySsaLabelCell.setCellStyle(totalStyle);

                Cell monthlySsaValueCell = monthlySsaRow.createCell(6);
                monthlySsaValueCell.setCellValue(agiResult.monthlySSACountableIncome);
                monthlySsaValueCell.setCellStyle(currencyStyle);

                // Add Non-Deductible Expenses row
                Row nonDeductibleRow = sheet.createRow(summaryStartRow + 8);
                Cell nonDeductibleLabelCell = nonDeductibleRow.createCell(5);
                nonDeductibleLabelCell.setCellValue("NON-DEDUCTIBLE EXPENSES");
                nonDeductibleLabelCell.setCellStyle(totalStyle);

                // Calculate non-deductible expenses
                double nonDeductibleExpenses = bills.stream()
                        .filter(bill -> !bill.isDeductible())
                        .mapToDouble(Bill::getAmount)
                        .sum();

                Cell nonDeductibleValueCell = nonDeductibleRow.createCell(6);
                nonDeductibleValueCell.setCellValue(-nonDeductibleExpenses);
                nonDeductibleValueCell.setCellStyle(negativeCurrencyStyle);

                // Add Cash Flow (Income after all bills) row
                Row cashFlowRow = sheet.createRow(summaryStartRow + 9);
                Cell cashFlowLabelCell = cashFlowRow.createCell(5);
                cashFlowLabelCell.setCellValue("AFTER ALL BILLS");
                cashFlowLabelCell.setCellStyle(totalStyle);

                // Calculate cash flow
                double totalBillAmount = bills.stream()
                        .mapToDouble(Bill::getAmount)
                        .sum();
                double incomeAfterAllBills = agiResult.grossIncome - totalBillAmount;

                Cell cashFlowValueCell = cashFlowRow.createCell(6);
                cashFlowValueCell.setCellValue(incomeAfterAllBills);

                CellStyle cashFlowStyle = workbook.createCellStyle();
                cashFlowStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
                cashFlowStyle.setFont(totalFont);
                cashFlowStyle.setBorderTop(BorderStyle.DOUBLE);
                cashFlowStyle.setBorderBottom(BorderStyle.DOUBLE);
                cashFlowValueCell.setCellStyle(cashFlowStyle);

                // Write the workbook to a file
                System.out.println("Writing Excel file to disk...");
                try (FileOutputStream fileOut = new FileOutputStream(exportPath.toFile())) {
                    workbook.write(fileOut);
                    fileOut.flush(); // Ensure all data is written
                    System.out.println("Excel file written successfully");
                } catch (IOException e) {
                    System.err.println("Failed to write Excel file: " + e.getMessage());
                    throw new IOException("Failed to write Excel file: " + e.getMessage());
                }
            }

            // Verify the file was created
            if (!Files.exists(exportPath)) {
                throw new IOException("Excel file was not created successfully");
            }

            // Return the path for confirmation message
            System.out.println("✅ Excel file exported successfully to: " + exportPath.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("Excel export failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Excel export failed: " + e.getMessage(), e);
        }
    }
}