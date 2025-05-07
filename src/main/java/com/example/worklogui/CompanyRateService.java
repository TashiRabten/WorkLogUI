package com.example.worklogui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class CompanyRateService {

    private static final CompanyRateService instance = new CompanyRateService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, RateInfo> rates = new LinkedHashMap<>();

    private CompanyRateService() {
        loadRates();  // Load on startup
    }

    public static CompanyRateService getInstance() {
        return instance;
    }

    public void loadRates() {
        try {
            if (Files.exists(AppConstants.RATES_PATH)) {
                Map<String, RateInfo> loaded = mapper.readValue(
                        AppConstants.RATES_PATH.toFile(),
                        new TypeReference<>() {}
                );
                rates.clear();
                rates.putAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveRates() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    AppConstants.RATES_PATH.toFile(), rates
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ New: Returns RateInfo directly
    public Map<String, RateInfo> getRateInfoMap() {
        return Collections.unmodifiableMap(rates);
    }

    // ✅ Compatibility: Returns just rates as doubles
    public Map<String, Double> getRates() {
        Map<String, Double> simpleMap = new LinkedHashMap<>();
        for (Map.Entry<String, RateInfo> entry : rates.entrySet()) {
            simpleMap.put(entry.getKey(), entry.getValue().getValor());
        }
        return simpleMap;
    }

    public void refreshRates() {
        loadRates();
    }

    public double getRateForCompany(String name) {
        return rates.getOrDefault(name, new RateInfo(0.0, "hour")).getValor();
    }

    public void setRate(String name, RateInfo info) {
        rates.put(name, info);
    }

    public void removeCompany(String name) {
        rates.remove(name);
    }
}
