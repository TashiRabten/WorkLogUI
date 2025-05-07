package com.example.worklogui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WarningUtils {

    private static final double LIMITE_MENSAL = 1600;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Generates a warning message if the current month's earnings approach or exceed the limit
     * @param registros List of work entries
     * @return A bilingual warning message or null if no warning is needed
     */
    public static String generateCurrentMonthWarning(List<RegistroTrabalho> registros) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        double totalThisMonth = registros.stream()
                .filter(r -> {
                    try {
                        LocalDate date = LocalDate.parse(r.getData(), DATE_FORMATTER);
                        return date.getYear() == currentYear && date.getMonthValue() == currentMonth;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .mapToDouble(r -> {
                    double g = r.getTipoUsado().equalsIgnoreCase("minuto")
                            ? r.getMinutos() * r.getTaxaUsada()
                            : r.getHoras() * r.getTaxaUsada();
                    return r.isPagamentoDobrado() ? g * 2 : g;
                })
                .sum();

        String formatted = String.format("$%.2f", totalThisMonth);

        if (totalThisMonth < LIMITE_MENSAL * 0.9) {
            return null; // No warning
        } else if (totalThisMonth < LIMITE_MENSAL) {
            return "✅ You are approaching (" + formatted + ") the $1600 monthly limit.\n" +
                    "✅ Está se aproximando (" + formatted + ") do limite mensal de $1600.";
        } else if (Math.abs(totalThisMonth - LIMITE_MENSAL) < 0.01) {
            return "🎯 You are exactly (" + formatted + ") at the $1600 monthly limit.\n" +
                    "🎯 Está exatamente (" + formatted + ") no limite mensal de $1600.";
        } else if (totalThisMonth <= LIMITE_MENSAL * 1.1) {
            return "⚠ You are up to 10% above (" + formatted + ") the $1600 limit.\n" +
                    "⚠ Está até 10% acima (" + formatted + ") do limite mensal de $1600.";
        } else if (totalThisMonth <= LIMITE_MENSAL * 1.2) {
            return "⚠ You are over 10% above (" + formatted + ") the $1600 limit.\n" +
                    "⚠ Ultrapassou 10% (" + formatted + ") do limite mensal de $1600.";
        } else if (totalThisMonth <= LIMITE_MENSAL * 1.3) {
            return "⚠ You are over 20% above (" + formatted + ") the $1600 limit.\n" +
                    "⚠ Ultrapassou 20% do (" + formatted + ") limite mensal de $1600.";
        } else {
            return "🚨 You are over 30% above (" + formatted + ") the $1600 limit.\n" +
                    "🚨 Ultrapassou 30% do (" + formatted + ") limite mensal de $1600.";
        }
    }

    /**
     * Creates a complete warning block for displaying at startup
     * @param registros List of work entries
     * @return A formatted warning block or null if no warning is needed
     */
    public static String generateStartupWarningBlock(List<RegistroTrabalho> registros) {
        String warning = generateCurrentMonthWarning(registros);
        if (warning == null) return null;

        Optional<LocalDate> latestDate = registros.stream()
                .map(r -> {
                    try {
                        return LocalDate.parse(r.getData(), DATE_FORMATTER);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());

        String label = latestDate
                .map(d -> d.format(DateTimeFormatter.ofPattern("MM/yyyy")))
                .orElse("Current Month / Mês Atual");

        return formatWarningBlock(warning, label);
    }

    /**
     * Adds a timestamp to a warning message
     * @param warningText The warning message text
     * @return A formatted warning block with timestamp
     */
    public static String appendTimestampedWarning(String warningText) {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        return formatWarningBlock(warningText, timestamp);
    }

    /**
     * Formats a warning message into a visual block with dividers
     * @param warning The warning message
     * @param label A label for the warning (date or period)
     * @return A formatted warning block
     */
    private static String formatWarningBlock(String warning, String label) {
        return "\n\n------------------------------\n" +
                "📅 " + label + " • Warning / Aviso:\n\n" +
                warning + "\n" +
                "------------------------------";
    }
}