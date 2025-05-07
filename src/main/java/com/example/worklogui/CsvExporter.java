package com.example.worklogui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CsvExporter {

    private static final DecimalFormat moneyFormat = new DecimalFormat("0.00");

    public static void exportToCsv(List<RegistroTrabalho> registros) throws IOException {
        Map<String, Map<String, double[]>> monthly = new TreeMap<>();
        Map<String, Map<String, double[]>> yearly = new TreeMap<>();
        Map<String, Double> totalMesGanho = new TreeMap<>();
        Map<String, Double> totalAnoGanho = new TreeMap<>();

        for (RegistroTrabalho r : registros) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);
            LocalDate date;
            try {
                date = LocalDate.parse(r.getData(), formatter);
            } catch (Exception e) {
                continue; // skip invalid dates
            }

            String monthKey = String.format("%d-%02d", date.getYear(), date.getMonthValue());
            String yearKey = String.valueOf(date.getYear());
            String empresa = r.getEmpresa();

            double taxa = r.getTaxaUsada();
            String tipo = r.getTipoUsado() != null ? r.getTipoUsado() : "hour";

            double ganho = tipo.equalsIgnoreCase("minuto")
                    ? r.getMinutos() * taxa
                    : r.getMinutos() * (taxa / 60.0) + r.getHoras() * taxa;

            if (r.isPagamentoDobrado()) ganho *= 2;

            // Monthly totals
            monthly.computeIfAbsent(monthKey, k -> new TreeMap<>())
                    .computeIfAbsent(empresa, k -> new double[3]);
            double[] m = monthly.get(monthKey).get(empresa);
            m[0] += r.getHoras();
            m[1] += r.getMinutos();
            m[2] += ganho;
            totalMesGanho.merge(monthKey, ganho, Double::sum);

            // Yearly totals
            yearly.computeIfAbsent(yearKey, k -> new TreeMap<>())
                    .computeIfAbsent(empresa, k -> new double[3]);
            double[] y = yearly.get(yearKey).get(empresa);
            y[0] += r.getHoras();
            y[1] += r.getMinutos();
            y[2] += ganho;
            totalAnoGanho.merge(yearKey, ganho, Double::sum);
        }

        Files.createDirectories(AppConstants.EXPORT_FOLDER);
        Path file = AppConstants.EXPORT_FOLDER.resolve("summary_full.csv");


        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // BOM for Excel
            writer.write("Company,Month,Year,Hours,Minutes,Total Hours,Earnings");
            writer.newLine();

            for (Map.Entry<String, Map<String, double[]>> monthEntry : monthly.entrySet()) {
                String[] dateParts = monthEntry.getKey().split("-");
                String year = dateParts[0];
                String month = dateParts[1];

                for (Map.Entry<String, double[]> e : monthEntry.getValue().entrySet()) {
                    double h = e.getValue()[0];
                    double m = e.getValue()[1];
                    double ganho = e.getValue()[2];
                    double totalHoras = h + m / 60.0;

                    writer.write(String.format(Locale.US, "%s,%s,%s,%.2f,%.2f,%.2f,%.2f\n",
                            e.getKey(), month, year, h, m, totalHoras, ganho));
                }

                double totalGanhoMes = totalMesGanho.getOrDefault(monthEntry.getKey(), 0.0);
                writer.write(String.format(Locale.US, "MONTH TOTAL,%s,%s,,,,%.2f\n", month, year, totalGanhoMes));
                writer.newLine();
            }

            writer.write("Company,YEAR,,,,,\n");
            for (Map.Entry<String, Map<String, double[]>> yearEntry : yearly.entrySet()) {
                String year = yearEntry.getKey();
                for (Map.Entry<String, double[]> e : yearEntry.getValue().entrySet()) {
                    double h = e.getValue()[0];
                    double m = e.getValue()[1];
                    double ganho = e.getValue()[2];
                    double totalHoras = h + m / 60.0;

                    writer.write(String.format(Locale.US, "%s,,%s,%.2f,%.2f,%.2f,%.2f\n",
                            e.getKey(), year, h, m, totalHoras, ganho));
                }

                double totalGanhoAno = totalAnoGanho.getOrDefault(year, 0.0);
                writer.write(String.format(Locale.US, "YEAR TOTAL,,%s,,,,%.2f\n", year, totalGanhoAno));
                writer.newLine();
            }
        }
    }
}
