package com.example.worklogui;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WarningUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final Font WARNING_FONT = new Font("Segoe UI", 12);

    // Track the last filtered month we showed a popup for
    private static String lastFilteredMonth = null;

    private static String generateWarningMessage(double monthlyNESE, double sgaLimit, String context) {
        String formatted = String.format("$%.2f", monthlyNESE);
        String limitStr = String.format("$%.0f", sgaLimit);

        if (monthlyNESE < sgaLimit * 0.9) {
            return null; // No warning
        } else if (monthlyNESE < sgaLimit) {
            return String.format("✅ Your NESE (Net Earnings from Self-Employment) is approaching (%s) the %s monthly SGA limit.\n" +
                            "✅ Seu NESE (Ganhos Líquidos de Trabalho Autônomo) está se aproximando (%s) do limite SGA mensal de %s.",
                    formatted, limitStr, formatted, limitStr);
        } else if (Math.abs(monthlyNESE - sgaLimit) < 0.01) {
            return String.format("🎯 Your NESE is exactly (%s) at the %s monthly SGA limit.\n" +
                            "🎯 Seu NESE está exatamente (%s) no limite SGA mensal de %s.",
                    formatted, limitStr, formatted, limitStr);
        } else if (monthlyNESE <= sgaLimit * 1.1) {
            return String.format("⚠ Your NESE is up to 10%% above (%s) the %s SGA limit.\n" +
                            "⚠ Seu NESE está até 10%% acima (%s) do limite SGA de %s.",
                    formatted, limitStr, formatted, limitStr);
        } else if (monthlyNESE <= sgaLimit * 1.2) {
            return String.format("⚠ Your NESE is over 10%% above (%s) the %s SGA limit.\n" +
                            "⚠ Seu NESE ultrapassou 10%% (%s) do limite SGA de %s.",
                    formatted, limitStr, formatted, limitStr);
        } else if (monthlyNESE <= sgaLimit * 1.3) {
            return String.format("⚠ Your NESE is over 20%% above (%s) the %s SGA limit.\n" +
                            "⚠ Seu NESE ultrapassou 20%% (%s) do limite SGA de %s.",
                    formatted, limitStr, formatted, limitStr);
        } else {
            return String.format("🚨 Your NESE is over 30%% above (%s) the %s SGA limit.\n" +
                            "🚨 Seu NESE ultrapassou 30%% (%s) do limite SGA de %s.",
                    formatted, limitStr, formatted, limitStr);
        }
    }

    public static String generateCurrentMonthWarning(List<RegistroTrabalho> registros) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        double sgaLimit = AGICalculator.getSGALimit(currentYear);

        // Get the service to calculate NESE
        CompanyManagerService service = new CompanyManagerService();
        try {
            service.initialize();

            // Get bills for current month
            String yearMonth = String.format("%d-%02d", currentYear, currentMonth);
            List<Bill> monthBills = service.getBillsForMonth(yearMonth);

            // Filter registros for current month
            List<RegistroTrabalho> currentMonthRegistros = new ArrayList<>();
            for (RegistroTrabalho r : registros) {
                try {
                    LocalDate date = LocalDate.parse(r.getData(), DATE_FORMATTER);
                    if (date.getYear() == currentYear && date.getMonthValue() == currentMonth) {
                        currentMonthRegistros.add(r);
                    }
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }

            // Calculate AGI - specify that data is monthly
            AGICalculator.AGIResult result = AGICalculator.calculateAGI(currentMonthRegistros, monthBills, true);
            double monthlyNESE = result.monthlySSACountableIncome;

            return generateWarningMessage(monthlyNESE, sgaLimit, "Current month");
        } catch (Exception e) {
            // Fall back to gross income calculation if something fails
            e.printStackTrace();
            return null;
        }
    }

    public static String generateFilteredWarning(List<RegistroTrabalho> registros, String selectedYear,
                                                 String selectedMonth) {
        // If "All" is selected for either year or month, we can't show a meaningful warning
        if ("All".equalsIgnoreCase(selectedYear) || "All".equalsIgnoreCase(selectedMonth)) {
            return null;
        }

        int year = Integer.parseInt(selectedYear);
        int month = Integer.parseInt(selectedMonth);
        double sgaLimit = AGICalculator.getSGALimit(year);

        // Get the service to calculate NESE
        CompanyManagerService service = new CompanyManagerService();
        try {
            service.initialize();
            String yearMonth = String.format("%d-%02d", year, month);
            List<Bill> monthBills = service.getBillsForMonth(yearMonth);

            // Filter registros for the selected month
            List<RegistroTrabalho> monthRegistros = new ArrayList<>();
            for (RegistroTrabalho r : registros) {
                try {
                    LocalDate date = LocalDate.parse(r.getData(), DATE_FORMATTER);
                    if (date.getYear() == year && date.getMonthValue() == month) {
                        monthRegistros.add(r);
                    }
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }

            // Calculate AGI - specify that data is monthly
            AGICalculator.AGIResult result = AGICalculator.calculateAGI(monthRegistros, monthBills, true);
            double monthlyNESE = result.monthlySSACountableIncome;

            String context = String.format("%02d/%d", month, year);
            return generateWarningMessage(monthlyNESE, sgaLimit, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

        String title = "SSDI NESE Monthly Warning / Aviso Mensal NESE SSDI";
        return showPopupWarning(title, warning, alertType);
    }

    public static boolean showFilteredPopupWarningIfNeeded(List<RegistroTrabalho> registros,
                                                           String selectedYear, String selectedMonth) {
        // If "All" is selected for either year or month, don't show a popup
        if ("All".equalsIgnoreCase(selectedYear) || "All".equalsIgnoreCase(selectedMonth)) {
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
        String title = "SSDI NESE Warning / Aviso NESE: " + selectedMonth + "/" + selectedYear;

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
                alert.getDialogPane().setMinWidth(480);
                alert.getDialogPane().setPrefWidth(480);

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