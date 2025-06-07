package com.example.worklogui;

import com.example.worklogui.utils.ValidationHelper;
import com.example.worklogui.utils.ErrorHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Updated WorkLogEntryController using new validation and error handling
 */
public class WorkLogEntryController {

    @FXML private DatePicker dateField;
    @FXML private ComboBox<String> jobTypeCombo;
    @FXML private TextField valueField;
    @FXML private CheckBox doublePayCheckBox;

    private final CompanyManagerService service;
    private Consumer<String> statusMessageHandler;
    private Consumer<String> warningMessageHandler;
    private Runnable onWorkLoggedCallback;

    // Track last added entry for filter updates
    private LocalDate lastAddedEntryDate;
    private String lastAddedCompany;

    public WorkLogEntryController(CompanyManagerService service) {
        this.service = service;
    }

    public void initialize() {
        reloadCompanyList();
    }

    public void setStatusMessageHandler(Consumer<String> handler) {
        this.statusMessageHandler = handler;
        // Set up error handler to use the same status handler
        ErrorHandler.setStatusMessageHandler(handler);
    }

    public void setWarningMessageHandler(Consumer<String> handler) {
        this.warningMessageHandler = handler;
    }

    public void setOnWorkLoggedCallback(Runnable callback) {
        this.onWorkLoggedCallback = callback;
    }

    public void setupControls(DatePicker dateField, ComboBox<String> jobTypeCombo,
                              TextField valueField, CheckBox doublePayCheckBox) {
        this.dateField = dateField;
        this.jobTypeCombo = jobTypeCombo;
        this.valueField = valueField;
        this.doublePayCheckBox = doublePayCheckBox;

        configureDatePicker();
        initialize();
    }

    private void configureDatePicker() {
        if (dateField != null) {
            // Set the converter to format dates as MM/dd/yyyy
            dateField.setConverter(new javafx.util.StringConverter<LocalDate>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

                @Override
                public String toString(LocalDate date) {
                    return date != null ? formatter.format(date) : "";
                }

                @Override
                public LocalDate fromString(String string) {
                    if (string == null || string.trim().isEmpty()) {
                        return null;
                    }
                    try {
                        return LocalDate.parse(string, formatter);
                    } catch (Exception e) {
                        return null;
                    }
                }
            });

            dateField.setValue(LocalDate.now());
            dateField.setPromptText("MM/DD/YYYY");
        }
    }

    public void reloadCompanyList() {
        if (jobTypeCombo != null) {
            jobTypeCombo.getItems().setAll(CompanyRateService.getInstance().getRates().keySet());
        }
    }

    public void onLogWork() {
        try {
            // Get input values
            LocalDate parsedDate = dateField.getValue();
            String empresa = jobTypeCombo.getValue();
            String rawValue = valueField.getText().trim();
            boolean dobro = doublePayCheckBox.isSelected();

            // Convert date to string for validation
            String dateString = parsedDate != null ? parsedDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "";

            // Validate inputs using ValidationHelper
            ValidationHelper.WorkLogValidationResult validation =
                    ValidationHelper.validateWorkLogEntry(dateString, empresa, rawValue);

            if (!validation.isValid()) {
                setStatusMessage(validation.getErrorMessage());
                return;
            }

            // Parse value
            double valor = Double.parseDouble(rawValue);

            // Log the work using the service
            RegistroTrabalho newEntry = service.logWork(parsedDate, empresa, valor, dobro);

            // Track the entry for filter updates
            lastAddedEntryDate = parsedDate;
            lastAddedCompany = empresa;

            // Clear input fields
            valueField.clear();
            doublePayCheckBox.setSelected(false);

            // Check for warnings - PASS THE SERVICE INSTANCE
            String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros(), service);
            if (warning != null) {
                setStatusMessage("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");
                setWarningMessage(warning);
                WarningUtils.resetTrackedMonth();
            } else {
                setStatusMessage("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");
            }

            // Notify callback
            if (onWorkLoggedCallback != null) {
                onWorkLoggedCallback.run();
            }

        } catch (NumberFormatException e) {
            ErrorHandler.handleValidationError("Work Log Entry", "Please enter a valid number for time worked.\nPor favor, digite um número válido para o tempo trabalhado.");
        } catch (Exception e) {
            ErrorHandler.handleUnexpectedError("Work Log Entry", e);
        }
    }

    public LocalDate getLastAddedEntryDate() {
        return lastAddedEntryDate;
    }

    public String getLastAddedCompany() {
        return lastAddedCompany;
    }

    private void setStatusMessage(String message) {
        if (statusMessageHandler != null) {
            statusMessageHandler.accept(message);
        }
    }

    private void setWarningMessage(String warning) {
        if (warningMessageHandler != null) {
            warningMessageHandler.accept(warning);
        }
    }
}