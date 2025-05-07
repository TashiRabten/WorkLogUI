package com.example.worklogui;

public class RegistroTrabalho {
    private String data;               // e.g., "06/05/2025"
    private String empresa;            // e.g., "SOSI"
    private double horas;              // e.g., 2.5
    private double minutos;            // e.g., 30
    private boolean pagamentoDobrado;  // true if it's a holiday or double-pay situation

    private double taxaUsada;          // e.g., 25.00 or 0.65
    private String tipoUsado;          // "hora" or "minuto"

    // Empty constructor for JSON
    public RegistroTrabalho() {}

    public RegistroTrabalho(String data, String empresa, double horas, double minutos, boolean pagamentoDobrado) {
        this.data = data;
        this.empresa = empresa;
        this.horas = horas;
        this.minutos = minutos;
        this.pagamentoDobrado = pagamentoDobrado;
    }

    // Getters and Setters
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public double getHoras() {
        return horas;
    }

    public void setHoras(double horas) {
        this.horas = horas;
    }

    public double getMinutos() {
        return minutos;
    }

    public void setMinutos(double minutos) {
        this.minutos = minutos;
    }

    public boolean isPagamentoDobrado() {
        return pagamentoDobrado;
    }

    public void setPagamentoDobrado(boolean pagamentoDobrado) {
        this.pagamentoDobrado = pagamentoDobrado;
    }

    public double getTaxaUsada() {
        return taxaUsada;
    }

    public void setTaxaUsada(double taxaUsada) {
        this.taxaUsada = taxaUsada;
    }

    public String getTipoUsado() {
        return tipoUsado;
    }

    public void setTipoUsado(String tipoUsado) {
        this.tipoUsado = tipoUsado;
    }
}
