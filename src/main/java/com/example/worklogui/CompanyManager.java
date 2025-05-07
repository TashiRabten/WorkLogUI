package com.example.worklogui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.example.worklogui.WarningUtils;


public class CompanyManager {

    @FXML
    private TextField dateField;
    @FXML
    private ComboBox<String> jobTypeCombo;
    @FXML
    private TextField valueField;
    @FXML
    private CheckBox doublePayCheckBox;
    @FXML
    private TextArea resultArea;
    @FXML
    private Button editCompaniesBtn;

    @FXML
    private Button openLogEditorBtn;

    private static final Path WORKLOG_PATH = FileLoader.getDefaultPath();

    private List<RegistroTrabalho> registros = new ArrayList<>();


    @FXML
    private void handleEditCompanies() {
        CompanyEditorUI editor = new CompanyEditorUI();
        editor.setOnClose(this::reloadCompanyList);  // ✅ Ensures combo updates
        editor.show((Stage) editCompaniesBtn.getScene().getWindow());
    }


    private void reloadCompanyList() {
        jobTypeCombo.getItems().setAll(CompanyRateService.getInstance().getRates().keySet());
    }

    @FXML
    private void onExportCsv() {
        try {
            CsvExporter.exportToCsv(registros);
            resultArea.setText("✔ Exportorted to the 'documents/exports' folder.\n✔ Exportado para a pasta 'documents/exports'.");
        } catch (Exception e) {
            resultArea.setText("❌ Error while exporting: " + e.getMessage() + "❌ Erro ao exportar: " + e.getMessage());
        }
    }

    @FXML
    public void onLogWork() {
        String rawDate = dateField.getText().trim();
        String empresa = jobTypeCombo.getValue();
        String rawValue = valueField.getText().trim();
        boolean dobro = doublePayCheckBox.isSelected();

        if (rawDate.isEmpty() || empresa == null || rawValue.isEmpty()) {
            resultArea.setText("All fields are required.\nTodos os campos são obrigatórios.");
            return;
        }

        LocalDate parsedDate;
        try {
            parsedDate = DateParser.parseDate(rawDate);
        } catch (DateTimeParseException e) {
            resultArea.setText("❌ Invalid date format. Use MM/DD/YYYY.\n❌ Formato de data inválido. Use MM/DD/AA.");
            return;
        }

        String data = parsedDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

        double valor;
        try {
            valor = Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            resultArea.setText("❌ Invalid time value: " + rawValue + "\n❌ Valor de tempo inválido.");
            return;
        }

        RateInfo info = CompanyRateService.getInstance()
                .getRateInfoMap()
                .getOrDefault(empresa, new RateInfo(0.0, "hour"));

        RegistroTrabalho novo = new RegistroTrabalho();
        novo.setData(data);
        novo.setEmpresa(empresa);
        novo.setPagamentoDobrado(dobro);
        novo.setTaxaUsada(info.getValor());
        novo.setTipoUsado(info.getTipo());

        if (info.getTipo().equalsIgnoreCase("minuto")) {
            novo.setHoras(0);
            novo.setMinutos(valor);
        } else {
            novo.setHoras(valor);
            novo.setMinutos(0);
        }

        registros.add(novo); // ✅ MUST come before calculating warning

        try {
            FileLoader.salvarRegistros(AppConstants.WORKLOG_PATH, registros);

            StringBuilder msg = new StringBuilder("✔ Work logged successfully.\n✔ Entrada registrada com sucesso.");

            String warning = WarningUtils.generateCurrentMonthWarning(registros);
            if (warning != null) {
                showAlert(Alert.AlertType.WARNING, "Monthly Income Warning", warning);
                msg.append(WarningUtils.appendTimestampedWarning(warning));
            }

            resultArea.setText(msg.toString());
        } catch (Exception e) {
            resultArea.setText("❌ Error saving entry:\n" + e.getMessage());
        }
    }

    /**
     * Opens the dedicated Log Editor in a new window
     */
    public void onOpenLogEditor() {
        try {
            System.out.println("Opening Log Editor...");

            // Create and show the log editor
            LogEditorUI editor = new LogEditorUI();

            // Set a callback to refresh our data when the editor is closed
            editor.setOnClose(() -> {
                try {
                    // Reload the registros list
                    registros = FileLoader.carregarRegistros(WORKLOG_PATH);

                    // Prepare the success message
                    StringBuilder msg = new StringBuilder();
                    msg.append("✔ Work logs have been updated!\n✔ Registros de trabalho atualizados!");

                    // Check for warnings and include them if present
                    String warning = WarningUtils.generateCurrentMonthWarning(registros);
                    if (warning != null) {
                        msg.append(WarningUtils.appendTimestampedWarning(warning));
                    }

                    // Replace the current text (don't append)
                    resultArea.setText(msg.toString());

                } catch (Exception e) {
                    resultArea.setText("❌ Error refreshing work logs: " + e.getMessage() +
                            "\n❌ Erro ao atualizar os registros de trabalho: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Show the editor
            editor.show((Stage) openLogEditorBtn.getScene().getWindow());

        } catch (Exception e) {
            resultArea.setText("❌ Error opening Log Editor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onShowTotal() {
        Map<String, double[]> totais = new HashMap<>();

        CompanyRateService.getInstance().getRateInfoMap().keySet().forEach(company ->
                totais.put(company, new double[2]));  // [horas, minutos]

        for (RegistroTrabalho r : registros) {
            String empresa = r.getEmpresa();
            totais.putIfAbsent(empresa, new double[2]);  // [horas, minutos]
            totais.get(empresa)[0] += r.getHoras();
            totais.get(empresa)[1] += r.getMinutos();
        }

        StringBuilder sb = new StringBuilder("Total Summary / Resumo Total:\n\n");

        for (Map.Entry<String, double[]> e : totais.entrySet()) {
            String empresa = e.getKey();
            double horas = e.getValue()[0];
            double minutos = e.getValue()[1];

            // Convert any excess minutes to hours
            if (minutos >= 60) {
                int horasFromMinutos = (int) (minutos / 60);
                horas += horasFromMinutos;
                minutos = minutos % 60;
            }

            double totalHoras = horas + (minutos / 60.0);

            RateInfo info = CompanyRateService.getInstance()
                    .getRateInfoMap()
                    .getOrDefault(empresa, new RateInfo(0.0, "hora"));

            String tipo = info.getTipo().toLowerCase(Locale.ROOT);

            // Always show both hours and minutes
            sb.append(String.format("%s: %.2f hours, %.2f minutes\n",
                    empresa, horas, minutos));

            if (tipo.equals("minuto")) {
                sb.append(String.format(" → Total in minutes / Total em minutos: %.2f\n", (horas * 60) + minutos));
            }

            sb.append(String.format(" → Total in hours / Total em horas: %.2f\n\n", totalHoras));
        }

        resultArea.setText(sb.toString());
    }

    @FXML
    public void onShowSummaryByMonthAndYear() {
        Map<String, Map<String, List<RegistroTrabalho>>> registrosPorMesEDia = new TreeMap<>();
        Map<String, Map<String, Double>> totaisPorAnoEMes = new TreeMap<>();

        Locale localeUS = Locale.US;
        NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(localeUS);
        DateTimeFormatter formatoUS = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        for (RegistroTrabalho r : registros) {
            LocalDate dataLocal;
            try {
                dataLocal = LocalDate.parse(r.getData(), formatoUS);
            } catch (Exception e) {
                continue; // skip badly formatted
            }

            String diaUS = dataLocal.format(formatoUS);
            String mes = dataLocal.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String ano = String.valueOf(dataLocal.getYear());

            registrosPorMesEDia
                    .computeIfAbsent(mes, k -> new TreeMap<>())
                    .computeIfAbsent(diaUS, k -> new ArrayList<>())
                    .add(r);

            double taxa = r.getTaxaUsada();
            String tipo = r.getTipoUsado();

            if (tipo == null) tipo = "hour";
            double ganho = tipo.equalsIgnoreCase("minuto")
                    ? r.getMinutos() * taxa
                    : r.getHoras() * taxa;

            if (r.isPagamentoDobrado()) ganho *= 2;

            totaisPorAnoEMes
                    .computeIfAbsent(ano, k -> new TreeMap<>())
                    .merge(mes, ganho, Double::sum);
        }

        StringBuilder sb = new StringBuilder("📅 Monthly Details / Detalhes Mensais:\n\n");

        for (String mes : registrosPorMesEDia.keySet()) {
            sb.append("🗓 " + mes + "\n");
            Map<String, List<RegistroTrabalho>> dias = registrosPorMesEDia.get(mes);
            for (String diaUS : dias.keySet()) {
                for (RegistroTrabalho r : dias.get(diaUS)) {
                    double taxa = r.getTaxaUsada();
                    String tipo = r.getTipoUsado();
                    double ganho = tipo.equalsIgnoreCase("minuto")
                            ? r.getMinutos() * taxa
                            : r.getHoras() * taxa;
                    if (r.isPagamentoDobrado()) ganho *= 2;

                    sb.append(String.format(" - %s: %s → %s\n",
                            diaUS, r.getEmpresa(), formatoMoeda.format(ganho)));
                }
            }
            sb.append("------------------------------\n");
        }

        sb.append("\n📆 Yearly Summary / Resumo Anual:\n\n");
        for (String ano : totaisPorAnoEMes.keySet()) {
            sb.append("📅 " + ano + "\n");
            Map<String, Double> meses = totaisPorAnoEMes.get(ano);
            for (String mes : meses.keySet()) {
                sb.append(String.format(" - %s → %s\n", mes, formatoMoeda.format(meses.get(mes))));
            }
            sb.append("------------------------------\n");
        }

        resultArea.setText(sb.toString());
    }


    @FXML
    public void onShowEarnings() {
        double total = 0;
        Map<String, Double> ganhos = new HashMap<>();

        for (RegistroTrabalho r : registros) {
            String empresa = r.getEmpresa();

            double taxa = r.getTaxaUsada();
            String tipo = r.getTipoUsado();
            if (tipo == null) tipo = "hour";

            double ganho = tipo.equalsIgnoreCase("minuto")
                    ? r.getMinutos() * taxa
                    : r.getHoras() * taxa;

            if (r.isPagamentoDobrado()) ganho *= 2;

            ganhos.put(empresa, ganhos.getOrDefault(empresa, 0.0) + ganho);
            total += ganho;
        }

        StringBuilder sb = new StringBuilder("Earnings / Ganho Total:\n\n");
        for (Map.Entry<String, Double> e : ganhos.entrySet()) {
            sb.append(String.format("%s: $%.2f\n", e.getKey(), e.getValue()));
        }
        sb.append(String.format("\nGrand Total / Total Geral: $%.2f", total));

        resultArea.setText(sb.toString());
    }

    // First, let's add a TableView to the UI in the CompanyManager class
    // Add these fields to the CompanyManager class

    @FXML
    private TableView<RegistroTrabalho> logTable;
    @FXML
    private Button editLogBtn;
    @FXML
    private Button deleteLogBtn;
    @FXML
    private Button clearAllLogsBtn;

    public void initialize() {
        try {
            FileLoader.inicializarArquivo(WORKLOG_PATH);
            registros = FileLoader.carregarRegistros(WORKLOG_PATH);

            // Ensure we load the latest rates
            CompanyRateService.getInstance().refreshRates();

            reloadCompanyList();

            // ✅ Show current month's income warning in resultArea
            String warning = WarningUtils.generateCurrentMonthWarning(registros);
            if (warning != null) {
                String block = WarningUtils.appendTimestampedWarning(warning);
                resultArea.setText(block);
            }

        } catch (Exception e) {
            resultArea.setText("Erro ao inicializar / Error initializing: " + e.getMessage());
        }
    }

    private void setupLogTable() {
        // Date column
        TableColumn<RegistroTrabalho, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getData()));
        dateCol.setPrefWidth(100);

        // Company column
        TableColumn<RegistroTrabalho, String> companyCol = new TableColumn<>("Company");
        companyCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getEmpresa()));
        companyCol.setPrefWidth(120);

        // Hours column
        TableColumn<RegistroTrabalho, String> hoursCol = new TableColumn<>("Hours");
        hoursCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.2f", cellData.getValue().getHoras())));
        hoursCol.setPrefWidth(70);

        // Minutes column
        TableColumn<RegistroTrabalho, String> minutesCol = new TableColumn<>("Minutes");
        minutesCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.0f", cellData.getValue().getMinutos())));
        minutesCol.setPrefWidth(70);

        // Double Pay column
        TableColumn<RegistroTrabalho, String> doublePayCol = new TableColumn<>("Double Pay");
        doublePayCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().isPagamentoDobrado() ? "Yes" : "No"));
        doublePayCol.setPrefWidth(80);

        // Earnings column
        TableColumn<RegistroTrabalho, String> earningsCol = new TableColumn<>("Earnings");
        earningsCol.setCellValueFactory(cellData -> {
            RegistroTrabalho r = cellData.getValue();
            RateInfo info = CompanyRateService.getInstance()
                    .getRateInfoMap()
                    .getOrDefault(r.getEmpresa(), new RateInfo(0.0, "hour"));

            double rate = info.getValor();
            double earnings;

            if (info.getTipo().equalsIgnoreCase("minuto")) {
                earnings = r.getMinutos() * rate;
            } else {
                earnings = r.getHoras() * rate;
            }

            if (r.isPagamentoDobrado()) {
                earnings *= 2;
            }

            return new javafx.beans.property.SimpleStringProperty(
                    String.format("$%.2f", earnings));
        });
        earningsCol.setPrefWidth(80);

        logTable.getColumns().addAll(dateCol, companyCol, hoursCol, minutesCol, doublePayCol, earningsCol);

        // Keep the double-click handler to open the dedicated editor
        logTable.setRowFactory(tv -> {
            TableRow<RegistroTrabalho> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onOpenLogEditor(); // Open the editor instead of directly editing
                }
            });
            return row;
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}