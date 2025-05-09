package com.example.worklogui;

import java.util.*;

public class WorkLogData {
    private List<RegistroTrabalho> registros = new ArrayList<>();
    private Map<String, List<Bill>> bills = new HashMap<>();

    public List<RegistroTrabalho> getRegistros() {
        return registros;
    }

    public void setRegistros(List<RegistroTrabalho> registros) {
        this.registros = registros;
    }

    public Map<String, List<Bill>> getBills() {
        return bills;
    }

    public void setBills(Map<String, List<Bill>> bills) {
        this.bills = bills;
    }
}
