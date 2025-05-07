package com.example.worklogui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FileLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    // Always write to a user-writable folder
    private static final Path RATES_PATH = Paths.get(
            System.getProperty("user.home"),
            "Documents", "WorkLog", "company-rates.json"
    );

    public static Path getDefaultPath() {
        return AppConstants.WORKLOG_PATH;
    }


    /**
     * Load work log entries from JSON file
     */
    public static List<RegistroTrabalho> carregarRegistros(Path path) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Save work log entries to JSON file
     */
    public static void salvarRegistros(Path path, List<RegistroTrabalho> registros) {
        try {
            Files.createDirectories(path.getParent()); // ✅ Ensure parent directory exists
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), registros);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensure the JSON file exists (create empty if not)
     */public static void inicializarArquivo(Path path) {
        if (path == null) {
            System.err.println("❌ Caminho nulo fornecido para inicializarArquivo");
            return;
        }

        try {
            Files.createDirectories(path.getParent()); // ✅ ensure parent directory exists

            if (!Files.exists(path)) {
                salvarRegistros(path, new ArrayList<>());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}


