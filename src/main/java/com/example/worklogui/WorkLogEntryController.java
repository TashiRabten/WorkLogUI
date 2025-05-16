package com.example.worklogui;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;


public class WorkLogEntryController {

    @FXML private DatePicker dateField;
    @FXML private ComboBox<String> jobTypeCombo;
    @FXML private TextField valueField;
    @FXML private CheckBox doublePayCheckBox;

    private final CompanyManagerService service;
    private Consumer<String> statusMessageHandler;
    private Consumer<String> warningMessageHandler;
    private Runnable onWorkLoggedCallback;

    public WorkLogEntryController(CompanyManagerService service) {
        this.service = service;
    }


    public void initialize() {
        reloadCompanyList();
    }


    public void setStatusMessageHandler(Consumer<String> handler) {
        this.statusMessageHandler = handler;
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

        // Configure the date picker with proper formatting
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

            // Set current date as default
            dateField.setValue(LocalDate.now());

            // Add prompt text
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
            // Validate inputs
            LocalDate parsedDate = dateField.getValue();
            String empresa = jobTypeCombo.getValue();
            String rawValue = valueField.getText().trim();
            boolean dobro = doublePayCheckBox.isSelected();

            if (parsedDate == null || empresa == null || rawValue.isEmpty()) {
                setStatusMessage("All fields are required.\nTodos os campos são obrigatórios.");
                return;
            }

            // Parse value
            double valor = Double.parseDouble(rawValue);

            // Log the work
            // In the onLogWork method, after logging work:
            RegistroTrabalho newEntry = service.logWork(parsedDate, empresa, valor, dobro);
            lastAddedEntryDate = parsedDate;
            lastAddedCompany = empresa;

            valueField.clear();
            doublePayCheckBox.setSelected(false);

            String warning = WarningUtils.generateCurrentMonthWarning(service.getRegistros());
            if (warning != null) {
                setStatusMessage("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");
                setWarningMessage(warning);

                // Reset the tracked month to ensure filter popups show for new data
                WarningUtils.resetTrackedMonth();
            } else {
                setStatusMessage("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");
            }

            if (onWorkLoggedCallback != null) {
                onWorkLoggedCallback.run();
            }
        } catch (NumberFormatException e) {
            setStatusMessage("❌ Error: Please enter a valid number for time worked.");
        } catch (Exception e) {
            setStatusMessage("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
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
    // Add a field to store the latest entry date
    private LocalDate lastAddedEntryDate;
    private String lastAddedCompany;


    // Add a method to get this date
    public LocalDate getLastAddedEntryDate() {
        return lastAddedEntryDate;
    }

    public String getLastAddedCompany() {
        return lastAddedCompany;
    }

}