package com.example.worklogui;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WarningUtils {

    private static final double LIMITE_MENSAL = 1600;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final Font WARNING_FONT = new Font("Segoe UI", 12);

    // Track the last filtered month we showed a popup for
    private static String lastFilteredMonth = null;

    private static String generateWarningMessage(double totalEarnings, String context) {
        String formatted = String.format("$%.2f", totalEarnings);

        if (totalEarnings < LIMITE_MENSAL * 0.9) {
            return null; // No warning
        } else if (totalEarnings < LIMITE_MENSAL) {
            return "✅ You are approaching (" + formatted + ") the $1600 monthly limit.\n" +
                    "✅ Está se aproximando (" + formatted + ") do limite mensal de $1600.";
        } else if (Math.abs(totalEarnings - LIMITE_MENSAL) < 0.01) {
            return "🎯 You are exactly (" + formatted + ") at the $1600 monthly limit.\n" +
                    "🎯 Está exatamente (" + formatted + ") no limite mensal de $1600.";
        } else if (totalEarnings <= LIMITE_MENSAL * 1.1) {
            return "⚠ You are up to 10% above (" + formatted + ") the $1600 limit.\n" +
                    "⚠ Está até 10% acima (" + formatted + ") do limite mensal de $1600.";
        } else if (totalEarnings <= LIMITE_MENSAL * 1.2) {
            return "⚠ You are over 10% above (" + formatted + ") the $1600 limit.\n" +
                    "⚠ Ultrapassou 10% (" + formatted + ") do limite mensal de $1600.";
        } else if (totalEarnings <= LIMITE_MENSAL * 1.3) {
            return "⚠ You are over 20% above (" + formatted + ") the $1600 limit.\n" +
                    "⚠ Ultrapassou 20% do (" + formatted + ") limite mensal de $1600.";
        } else {
            return "🚨 You are over 30% above (" + formatted + ") the $1600 limit.\n" +
                    "🚨 Ultrapassou 30% do (" + formatted + ") limite mensal de $1600.";
        }
    }

    private static double calculateTotalEarnings(List<RegistroTrabalho> registros, int year, int month) {
        return registros.stream()
                .filter(r -> {
                    try {
                        LocalDate date = LocalDate.parse(r.getData(), DATE_FORMATTER);
                        return date.getYear() == year && date.getMonthValue() == month;
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
    }

    public static String generateCurrentMonthWarning(List<RegistroTrabalho> registros) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        double totalThisMonth = calculateTotalEarnings(registros, currentYear, currentMonth);
        String context = "Current month";

        return generateWarningMessage(totalThisMonth, context);
    }

    public static String generateFilteredWarning(List<RegistroTrabalho> registros, String selectedYear, String selectedMonth) {
        // If "All" is selected, we can't show a meaningful warning
        if ("All".equals(selectedYear) || "All".equals(selectedMonth)) {
            return null;
        }

        int year = Integer.parseInt(selectedYear);
        int month = Integer.parseInt(selectedMonth);

        double totalForMonth = calculateTotalEarnings(registros, year, month);
        String context = month + "/" + year;

        return generateWarningMessage(totalForMonth, context);
    }

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

    public static String appendTimestampedWarning(String warningText) {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        return formatWarningBlock(warningText, timestamp);
    }

    private static String formatWarningBlock(String warning, String label) {
        return "\n\n------------------------------\n" +
                "📅 " + label + " • Warning / Aviso:\n\n" +
                warning + "\n" +
                "------------------------------";
    }

    public static boolean showStartupWarningIfNeeded(List<RegistroTrabalho> registros) {
        String warning = generateCurrentMonthWarning(registros);
        if (warning == null) return false;

        // Determine severity based on content of warning message
        AlertType alertType = warning.contains("🚨") ? AlertType.ERROR :
                warning.contains("⚠") ? AlertType.WARNING :
                        AlertType.INFORMATION;

        String title = "Monthly Earnings Warning / Aviso de Faturamento";
        return showPopupWarning(title, warning, alertType);
    }

    public static boolean showFilteredPopupWarningIfNeeded(List<RegistroTrabalho> registros,
                                                           String selectedYear, String selectedMonth) {
        // If "All" is selected, don't show a popup
        if ("All".equals(selectedYear) || "All".equals(selectedMonth)) {
            return false;
        }

        // Generate a key for this month
        String monthKey = selectedYear + "-" + selectedMonth;

        // If we've already shown a popup for this month, don't show again
        if (monthKey.equals(lastFilteredMonth)) {
            return false;
        }

        // Get warning for this month
        String warning = generateFilteredWarning(registros, selectedYear, selectedMonth);
        if (warning == null) return false;

        // Only show popups for more serious warnings (over 10% of limit)
        if (!warning.contains("over 10%") && !warning.contains("over 20%") && !warning.contains("over 30%")) {
            return false;
        }

        // Set alert type based on severity
        AlertType alertType = warning.contains("🚨") ? AlertType.ERROR : AlertType.WARNING;

        // Create title
        String title = "Filter Warning / Aviso: " + selectedMonth + "/" + selectedYear;

        // Show the popup
        boolean shown = showPopupWarning(title, warning, alertType);

        // If shown, track this month
        if (shown) {
            lastFilteredMonth = monthKey;
        }

        return shown;
    }

    public static void resetTrackedMonth() {
        lastFilteredMonth = null;
    }

    private static boolean showPopupWarning(String title, String message, AlertType alertType) {
        try {
            // Create the alert on the JavaFX application thread
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(alertType, message, ButtonType.OK);
                alert.setTitle(title);
                alert.setHeaderText(null);  // No header text, keep it clean

                // Style the dialog content
                Label label = new Label(message);
                label.setFont(WARNING_FONT);
                label.setWrapText(true);
                alert.getDialogPane().setContent(label);

                // Make the dialog wider to accommodate the message
                alert.getDialogPane().setMinWidth(420);
                alert.getDialogPane().setPrefWidth(420);

                // Get the stage to customize further if needed
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.setResizable(true);

                // Show the alert
                alert.show();
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}