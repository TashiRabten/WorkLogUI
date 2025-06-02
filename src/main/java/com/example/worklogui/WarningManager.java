package com.example.worklogui;

import javafx.application.Platform;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages warning messages and alerts
 */
public class WarningManager {

    private final CompanyManagerService service;
    private Consumer<String> warningMessageHandler;
    private String currentWarning = null;

    public WarningManager(CompanyManagerService service) {
        this.service = service;
    }

    /**
     * Set callback for warning messages
     */
    public void setWarningMessageHandler(Consumer<String> handler) {
        this.warningMessageHandler = handler;
    }

    /**
     * Clear the current warning
     */
    public void clearWarning() {
        this.currentWarning = null;
        updateWarningDisplay();
    }

    /**
     * Set a warning message
     */
    public void setWarning(String warning) {
        this.currentWarning = warning;
        updateWarningDisplay();
    }

    /**
     * Get the current warning message
     */
    public String getCurrentWarning() {
        return currentWarning;
    }

    /**
     * Show startup warnings based on current data
     */
    public void showStartupWarnings() {
        String warning = WarningUtils.generateStartupWarningBlock(service.getRegistros());
        if (warning != null) {
            setWarning(warning);

            // Show startup popup for current month
            Platform.runLater(() -> {
                WarningUtils.showStartupWarningIfNeeded(service.getRegistros());
            });
        }
    }

    /**
     * Check for warnings based on filter selection
     */
    public void checkFilterWarnings(String selectedYear, String selectedMonth) {
        // Don't show warnings when "All" is selected for either year or month
        if (!"All".equals(selectedYear) && !"All".equals(selectedMonth)) {
            // This is for specific year and month filter - pass the service instance
            String filterWarning = WarningUtils.generateFilteredWarning(service.getRegistros(), selectedYear, selectedMonth, service);

            if (filterWarning != null) {
                // If there's a warning, show it
                setWarning(filterWarning);

                // Show popup for filtered month if needed
                Platform.runLater(() -> {
                    WarningUtils.showFilteredPopupWarningIfNeeded(service.getRegistros(), selectedYear, selectedMonth);
                });
            } else {
                // No warning needed, clear any existing warning
                clearWarning();
            }
        } else {
            // This is for "All" selection - do not show warnings
            clearWarning();
        }
    }

    /**
     * Check for warnings after logging new work
     */
    public void checkWarningsAfterLogWork() {
        // Pass the service instance to ensure proper bill loading
        String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros(), service);
        if (warning != null) {
            setWarning(warning);
            // Reset the tracked month to ensure filter popups show for new data
            WarningUtils.resetTrackedMonth();
        } else {
            clearWarning();
        }
    }

    /**
     * Show warnings for a specific month and year if needed
     */
    public void checkSpecificMonthWarning(String year, String month) {
        if (year == null || month == null) return;

        // Check if this is the current month
        LocalDate now = LocalDate.now();
        String currentYear = String.valueOf(now.getYear());
        String currentMonth = String.format("%02d", now.getMonthValue());

        if (year.equals(currentYear) && month.equals(currentMonth)) {
            // Pass the service instance to ensure proper bill loading
            String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros(), service);
            if (warning != null) {
                setWarning(WarningUtils.appendTimestampedWarning(warning));
            } else {
                clearWarning();
            }
        }
    }

    private void updateWarningDisplay() {
        if (warningMessageHandler != null) {
            warningMessageHandler.accept(currentWarning);
        }
    }
}