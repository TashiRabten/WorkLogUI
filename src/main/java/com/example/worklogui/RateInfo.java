package com.example.worklogui;

public class RateInfo {
    private double valor;
    private String tipo; // "hora" or "minuto"

    public RateInfo() {}

    public RateInfo(double valor, String tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
