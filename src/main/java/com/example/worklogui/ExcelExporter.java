package com.example.worklogui;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExcelExporter {

    public static void exportToExcel(List<DisplayEntry> entries, CompanyManagerService service,boolean isAllExport) throws IOException {
        // Create directory if it doesn't exist
        Files.createDirectories(AppConstants.EXPORT_FOLDER);


        // Generate filename with a descriptive name and formatted timestamp
        Files.createDirectories(AppConstants.EXPORT_FOLDER);

        // Generate filename with a descriptive name and formatted timestamp
        String filePrefix = isAllExport ? "all_summary_with_bills" : "summary_with_bills";
        String timestamp = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").format(LocalDateTime.now());
        Path exportPath = AppConstants.EXPORT_FOLDER.resolve(filePrefix + "_" + timestamp + ".xlsx");
        // Sort entries by date before exporting
        entries.sort(Comparator.comparing(DisplayEntry::getDate));

        // Track totals
        double grossTotal = 0.0;
        double billTotal = 0.0;

        // Create workbook and sheet
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("WorkLog Export");

            // Set column widths
            sheet.setColumnWidth(0, 12 * 256); // Type
            sheet.setColumnWidth(1, 20 * 256); // Category
            sheet.setColumnWidth(2, 12 * 256); // Date
            sheet.setColumnWidth(3, 10 * 256); // Hours
            sheet.setColumnWidth(4, 10 * 256); // Minutes
            sheet.setColumnWidth(5, 12 * 256); // Total Hours
            sheet.setColumnWidth(6, 15 * 256); // Earnings

            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Currency style with 3 decimals
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.000"));

            // Negative currency style with 3 decimals
            CellStyle negativeCurrencyStyle = workbook.createCellStyle();
            negativeCurrencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.000"));
            Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            negativeCurrencyStyle.setFont(redFont);

            // Number style with 3 decimals
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("0.000"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("mm/dd/yyyy"));

            CellStyle totalStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setBorderTop(BorderStyle.THIN);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"TYPE", "CATEGORY", "DATE", "HOURS", "MINUTES", "TOTAL HOURS", "EARNINGS"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            int rowNum = 1;
            for (DisplayEntry entry : entries) {
                Row row = sheet.createRow(rowNum++);

                if (entry.isBill()) {
                    Bill bill = entry.getBill();
                    billTotal += bill.getAmount();

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

                    // Hours - with 3 decimal places
                    Cell hoursCell = row.createCell(3);
                    hoursCell.setCellValue(r.getHoras());
                    hoursCell.setCellStyle(numberStyle);

                    // Minutes
                    row.createCell(4).setCellValue(r.getMinutos());

                    // Total Hours - with 3 decimal places
                    Cell totalHoursCell = row.createCell(5);
                    totalHoursCell.setCellValue(totalHours);
                    totalHoursCell.setCellStyle(numberStyle);

                    // Earnings - with 3 decimal places
                    Cell earningsCell = row.createCell(6);
                    earningsCell.setCellValue(earnings);
                    earningsCell.setCellStyle(currencyStyle);
                }
            }

            double netTotal = grossTotal - billTotal;

            // Add summary rows
            int summaryStartRow = rowNum + 1;

            // Gross Total
            Row grossRow = sheet.createRow(summaryStartRow);
            Cell grossLabelCell = grossRow.createCell(5);
            grossLabelCell.setCellValue("GROSS TOTAL");
            grossLabelCell.setCellStyle(totalStyle);

            Cell grossValueCell = grossRow.createCell(6);
            grossValueCell.setCellValue(grossTotal);
            grossValueCell.setCellStyle(currencyStyle);

            // Bill Total
            Row billRow = sheet.createRow(summaryStartRow + 1);
            Cell billLabelCell = billRow.createCell(5);
            billLabelCell.setCellValue("TOTAL BILLS");
            billLabelCell.setCellStyle(totalStyle);

            Cell billValueCell = billRow.createCell(6);
            billValueCell.setCellValue(-billTotal);
            billValueCell.setCellStyle(negativeCurrencyStyle);

            // Net Total
            Row netRow = sheet.createRow(summaryStartRow + 2);
            Cell netLabelCell = netRow.createCell(5);
            netLabelCell.setCellValue("NET TOTAL");
            netLabelCell.setCellStyle(totalStyle);

            Cell netValueCell = netRow.createCell(6);
            netValueCell.setCellValue(netTotal);

            CellStyle netTotalStyle = workbook.createCellStyle();
            netTotalStyle.setDataFormat(dataFormat.getFormat("$#,##0.000"));
            netTotalStyle.setFont(totalFont);
            netTotalStyle.setBorderTop(BorderStyle.DOUBLE);
            netValueCell.setCellStyle(netTotalStyle);

            // Write the workbook to a file
            try (FileOutputStream fileOut = new FileOutputStream(exportPath.toFile())) {
                workbook.write(fileOut);
            }
        }

        // Return the path for confirmation message
        System.out.println("✅ Excel file exported to: " + exportPath);
    }
}