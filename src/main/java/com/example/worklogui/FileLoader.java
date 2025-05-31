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

/**
 * File loader now focused only on bills - work logs are handled by WorkLogFileManager
 */
public class FileLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Load bills from a specific file
     */
    public static List<Bill> carregarBills(Path path) {
        if (!Files.exists(path)) {
            System.out.println("Bill file doesn't exist: " + path);
            return new ArrayList<>();
        }

        try {
            System.out.println("Loading bills from: " + path);
            String content = Files.readString(path);
            System.out.println("File content length: " + content.length());

            List<Bill> bills = objectMapper.readValue(path.toFile(), new TypeReference<List<Bill>>() {});
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

    /**
     * Save bills to a specific file
     */
    public static boolean salvarBills(Path path, List<Bill> bills) {
        try {
            Files.createDirectories(path.getParent());

            System.out.println("Saving " + bills.size() + " bills to: " + path);

            // Convert to JSON
            String json = objectMapper.writeValueAsString(bills);
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

    // Legacy methods for backward compatibility - these methods are now deprecated
    // and will be removed in future versions

    /**
     * @deprecated Use WorkLogFileManager instead
     */
    @Deprecated
    public static Path getDefaultPath() {
        return AppConstants.WORKLOG_PATH;
    }

    /**
     * @deprecated Use WorkLogFileManager instead
     */
    @Deprecated
    public static void inicializarArquivo(Path path) {
        System.out.println("⚠️  WARNING: inicializarArquivo is deprecated. Work logs are now managed by WorkLogFileManager.");
        // Do nothing - work logs are handled by WorkLogFileManager
    }

    /**
     * @deprecated Use WorkLogFileManager instead
     */
    @Deprecated
    public static WorkLogData carregarTudo(Path path) {
        System.out.println("⚠️  WARNING: carregarTudo is deprecated. Work logs are now managed by WorkLogFileManager.");
        return new WorkLogData(); // Return empty data
    }

    /**
     * @deprecated Use WorkLogFileManager instead
     */
    @Deprecated
    public static void salvarTudo(Path path, WorkLogData data) {
        System.out.println("⚠️  WARNING: salvarTudo is deprecated. Work logs are now managed by WorkLogFileManager.");
        // Do nothing - work logs are handled by WorkLogFileManager
    }

    /**
     * @deprecated Use WorkLogFileManager instead
     */
    @Deprecated
    public static List<RegistroTrabalho> carregarRegistros(Path path) {
        System.out.println("⚠️  WARNING: carregarRegistros is deprecated. Use CompanyManagerService.getRegistros() instead.");
        return new ArrayList<>(); // Return empty list
    }

    /**
     * @deprecated Use WorkLogFileManager instead
     */
    @Deprecated
    public static void salvarRegistros(Path path, List<RegistroTrabalho> registros) {
        System.out.println("⚠️  WARNING: salvarRegistros is deprecated. Use CompanyManagerService methods instead.");
        // Do nothing - work logs are handled by WorkLogFileManager
    }
}