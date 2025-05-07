package com.example.worklogui;

public class WorkLogEntry {
    private String date; // Format: MM/DD/YYYY
    private double sosiHours;
    private double lionBridgeMinutes;

    // Constructor
    public WorkLogEntry(String date) {
        this.date = date;
        this.sosiHours = 0.0;
        this.lionBridgeMinutes = 0.0;
    }

    // Getters
    public String getDate() {
        return date;
    }

    public double getSosiHours() {
        return sosiHours;
    }

    public double getLionBridgeMinutes() {
        return lionBridgeMinutes;
    }

    // Setters
    public void setSosiHours(double sosiHours) {
        this.sosiHours = sosiHours;
    }

    public void setLionBridgeMinutes(double lionBridgeMinutes) {
        this.lionBridgeMinutes = lionBridgeMinutes;
    }

    // Methods to increment values
    public void addSosiHours(double hours) {
        this.sosiHours += hours;
    }

    public void addLionBridgeMinutes(double minutes) {
        this.lionBridgeMinutes += minutes;
    }
}
