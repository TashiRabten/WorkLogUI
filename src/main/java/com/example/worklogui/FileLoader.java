package com.example.worklogui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), registros);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensure the JSON file exists (create empty if not)
     */
    public static void inicializarArquivo(Path path) {
        if (!Files.exists(path)) {
            salvarRegistros(path, new ArrayList<>());
        }
    }
}
