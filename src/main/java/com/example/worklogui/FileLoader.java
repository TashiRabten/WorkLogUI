package com.example.worklogui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class FileLoader {


    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Optional: makes dates readable

    private static final Path RATES_PATH = Paths.get(
            System.getProperty("user.home"),
            "Documents", "WorkLog", "company-rates.json"
    );

    public static Path getDefaultPath() {
        return AppConstants.WORKLOG_PATH;
    }

    private static WorkLogData cache = null;

    public static void inicializarArquivo(Path path) {
        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                salvarTudo(path, new WorkLogData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static WorkLogData carregarTudo(Path path) {
        if (!Files.exists(path)) return new WorkLogData();

        try {
            // Try loading full structure
            return objectMapper.readValue(path.toFile(), WorkLogData.class);
        } catch (Exception e) {
            // Fallback: try to load as raw list
            try {
                List<RegistroTrabalho> rawList = objectMapper.readValue(path.toFile(), new TypeReference<>() {});
                WorkLogData fallback = new WorkLogData();
                fallback.setRegistros(rawList);
                fallback.setBills(new HashMap<>());
                salvarTudo(path, fallback); // upgrade file
                return fallback;
            } catch (Exception inner) {
                inner.printStackTrace();
                return new WorkLogData();
            }
        }
    }


    public static void salvarTudo(Path path, WorkLogData data) {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
            cache = data;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper accessors
    public static List<RegistroTrabalho> carregarRegistros(Path path) {
        return carregarTudo(path).getRegistros();
    }

    public static void salvarRegistros(Path path, List<RegistroTrabalho> registros) {
        WorkLogData data = carregarTudo(path);
        data.setRegistros(registros);
        salvarTudo(path, data);
    }

    public static List<Bill> carregarBills(Path path) {
        if (!Files.exists(path)) return new ArrayList<>();
        try {
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static boolean salvarBills(Path path, List<Bill> bills) {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), bills);
            return true;
        } catch (IOException e) {
            System.out.println("❌ DIRECT ERROR: Failed to write bills to " + path.getFileName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}
