package com.example.worklogui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DisplayEntry {
    private RegistroTrabalho registro;
    private Bill bill;

    public DisplayEntry(RegistroTrabalho registro) {
        this.registro = registro;
    }

    public DisplayEntry(Bill bill) {
        this.bill = bill;
    }

    public boolean isBill() {
        return bill != null;
    }

    public LocalDate getDate() {
        if (registro != null) {
            return LocalDate.parse(registro.getData(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        }
        return bill.getDate();
    }

    public String getDateFormatted() {
        return getDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    public String getLabel() {
        if (isBill()) {
            // For bills, try both getLabel() and getDescription()
            String label = bill.getLabel();
            if (label == null || label.isEmpty()) {
                label = bill.getDescription();
            }
            return label != null ? label : "Unknown Bill";
        }
        return registro != null ? registro.getEmpresa() : "Unknown";
    }
    public double getHoras() {
        return registro != null ? registro.getHoras() : 0;
    }

    public double getMinutos() {
        return registro != null ? registro.getMinutos() : 0;
    }

    public boolean isPagamentoDobrado() {
        return registro != null && registro.isPagamentoDobrado();
    }

    public String getFormattedEarnings(CompanyManagerService service) {
        if (isBill()) {
            return String.format("-$%.2f", bill.getAmount());
        } else {
            double earnings = service.calculateEarnings(registro);
            return String.format("$%.2f", earnings);
        }
    }

    public RegistroTrabalho getRegistro() {
        return registro;
    }

    public Bill getBill() {
        return bill;
    }


}
