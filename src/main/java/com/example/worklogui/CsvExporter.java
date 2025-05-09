package com.example.worklogui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CsvExporter {

    public static void exportToCsv(List<DisplayEntry> entries, CompanyManagerService service) throws IOException {
        Files.createDirectories(AppConstants.EXPORT_FOLDER);
        Path file = AppConstants.EXPORT_FOLDER.resolve("summary_with_bills.csv");

        // Sort entries by date before exporting
        entries.sort(Comparator.comparing(DisplayEntry::getDate));

        double grossTotal = 0.0;
        double billTotal = 0.0;

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // Excel BOM
            writer.write("TYPE,CATEGORY,DATE,HOURS,MINUTES,TOTAL HOURS,EARNINGS");
            writer.newLine();

            for (DisplayEntry entry : entries) {
                if (entry.isBill()) {
                    Bill bill = entry.getBill();
                    billTotal += bill.getAmount();
                    writer.write(String.format(
                            Locale.US,
                            "Bill,%s,%s,,,,-$%.2f\n",
                            bill.getLabel(),
                            bill.getDate(),
                            bill.getAmount()
                    ));
                } else {
                    RegistroTrabalho r = entry.getRegistro();
                    double earnings = service.calculateEarnings(r);
                    grossTotal += earnings;
                    double totalHours = r.getHoras() + r.getMinutos() / 60.0;

                    writer.write(String.format(
                            Locale.US,
                            "Work,%s,%s,%.2f,%.0f,%.2f,$%.2f\n",
                            r.getEmpresa(),
                            r.getData(),
                            r.getHoras(),
                            r.getMinutos(),
                            totalHours,
                            earnings
                    ));
                }
            }

            double netTotal = grossTotal - billTotal;

            writer.newLine();
            writer.write(String.format(",,,,,GROSS TOTAL,$%.2f\n", grossTotal));
            writer.write(String.format(",,,,,TOTAL BILLS,-$%.2f\n", billTotal));
            writer.write(String.format(",,,,,NET TOTAL,$%.2f\n", netTotal));
        }
    }
}
