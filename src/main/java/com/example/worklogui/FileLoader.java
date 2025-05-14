package com.example.worklogui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.fasterxml.jackson.databind.DeserializationFeature;
import java.nio.file.StandardCopyOption;

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
        if (!Files.exists(path)) {
            System.out.println("Bill file doesn't exist: " + path);
            return new ArrayList<>();
        }

        try {
            System.out.println("Loading bills from: " + path);
            String content = Files.readString(path);
            System.out.println("File content length: " + content.length());

            // Create a special ObjectMapper for legacy support
            ObjectMapper billMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<Bill> bills = billMapper.readValue(path.toFile(), new TypeReference<List<Bill>>() {});
            System.out.println("Loaded " + bills.size() + " bills");

            // Initialize categories for all bills after loading
            for (Bill bill : bills) {
                System.out.println("  Processing bill: " + bill.getDescription());
                System.out.println("    Legacy deductible: " + bill.getLegacyDeductible());
                System.out.println("    Category before init: " + bill.getCategory());

                bill.initializeCategory();

                System.out.println("    Category after init: " + bill.getCategory());
                System.out.println("    Is deductible: " + bill.isDeductible());
            }

            return bills;
        } catch (IOException e) {
            System.err.println("Error loading bills from " + path + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    public static boolean salvarBills(Path path, List<Bill> bills) {
        try {
            Files.createDirectories(path.getParent());

            System.out.println("Saving " + bills.size() + " bills to: " + path);

            // Create ObjectMapper with proper configuration
            ObjectMapper billMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .enable(SerializationFeature.INDENT_OUTPUT);

            // Convert to JSON
            String json = billMapper.writeValueAsString(bills);
            System.out.println("JSON to save (" + json.length() + " chars): " +
                    (json.length() > 200 ? json.substring(0, 200) + "..." : json));

            // Write to file using atomic operation
            Path tempFile = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tempFile, json);
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("Successfully saved bills to: " + path);

            // Verify the file was created
            if (Files.exists(path)) {
                long fileSize = Files.size(path);
                System.out.println("File created successfully, size: " + fileSize + " bytes");

                // Verify content
                String savedContent = Files.readString(path);
                System.out.println("Saved content preview: " +
                        (savedContent.length() > 100 ? savedContent.substring(0, 100) + "..." : savedContent));

                return fileSize > 10; // Ensure file has actual content
            } else {
                System.err.println("File was not created!");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to save bills to " + path + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}
