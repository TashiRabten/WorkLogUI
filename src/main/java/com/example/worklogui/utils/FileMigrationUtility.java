package com.example.worklogui.utils;

import com.example.worklogui.RegistroTrabalho;
import com.example.worklogui.WorkLogData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility to migrate old WorkLogData format to new individual file format
 */
public class FileMigrationUtility {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Check if a file is in the old WorkLogData format or new array format
     */
    public static boolean isOldFormat(Path filePath) {
        if (!Files.exists(filePath)) {
            return false;
        }

        try {
            String content = Files.readString(filePath);
            // Old format starts with { and has "registros" property
            // New format starts with [ (array)
            return content.trim().startsWith("{") && content.contains("\"registros\"");
        } catch (IOException e) {
            System.err.println("Error checking file format: " + e.getMessage());
            return false;
        }
    }

    /**
     * Migrate a single file from old format to new format
     */
    public static List<RegistroTrabalho> migrateFile(Path oldFormatFile) throws IOException {
        System.out.println("🔄 Migrating file from old format: " + oldFormatFile);

        try {
            // Try to read as old WorkLogData format
            WorkLogData oldData = objectMapper.readValue(oldFormatFile.toFile(), WorkLogData.class);
            List<RegistroTrabalho> registros = oldData.getRegistros();

            if (registros == null) {
                registros = new ArrayList<>();
            }

            System.out.println("✅ Successfully migrated " + registros.size() + " work logs from old format");
            return registros;

        } catch (Exception e) {
            System.err.println("❌ Failed to migrate file: " + e.getMessage());
            throw new IOException("Failed to migrate old format file: " + oldFormatFile, e);
        }
    }

    /**
     * Attempt to load work logs from a file, handling both old and new formats
     */
    public static List<RegistroTrabalho> loadWorkLogsWithMigration(Path filePath) throws ErrorHandler.FileOperationException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try {
            // First, check if it's the old format
            if (isOldFormat(filePath)) {
                System.out.println("🔄 Detected old format file, migrating: " + filePath);

                // Migrate the file
                List<RegistroTrabalho> migratedLogs = migrateFile(filePath);

                // Save in new format
                objectMapper.writeValue(filePath.toFile(), migratedLogs);
                System.out.println("✅ File migrated to new format: " + filePath);

                return migratedLogs;
            } else {
                // Try to read as new format (array)
                System.out.println("📖 Reading file in new format: " + filePath);
                return objectMapper.readValue(filePath.toFile(), new TypeReference<List<RegistroTrabalho>>() {});
            }

        } catch (IOException e) {
            throw ErrorHandler.createFileException("load", filePath.getFileName().toString(), e);
        }
    }

    /**
     * Split work logs by year-month and save to individual files
     */
    public static void splitLogsIntoMonthlyFiles(List<RegistroTrabalho> allLogs, Path logsDirectory) throws IOException {
        System.out.println("🔄 Splitting " + allLogs.size() + " logs into monthly files");

        // Group logs by year-month
        Map<String, List<RegistroTrabalho>> groupedLogs = new HashMap<>();

        for (RegistroTrabalho log : allLogs) {
            String yearMonthKey = DateUtils.getYearMonthKeyFromDateString(log.getData());
            if (yearMonthKey != null) {
                groupedLogs.computeIfAbsent(yearMonthKey, k -> new ArrayList<>()).add(log);
            } else {
                System.err.println("⚠️ Skipping log with invalid date: " + log.getData());
            }
        }

        // Create logs directory if it doesn't exist
        Files.createDirectories(logsDirectory);

        // Save each month's logs to a separate file
        for (Map.Entry<String, List<RegistroTrabalho>> entry : groupedLogs.entrySet()) {
            String yearMonth = entry.getKey();
            List<RegistroTrabalho> monthLogs = entry.getValue();

            Path monthFile = logsDirectory.resolve(yearMonth + ".json");
            objectMapper.writeValue(monthFile.toFile(), monthLogs);

            System.out.println("💾 Saved " + monthLogs.size() + " logs to " + monthFile);
        }

        System.out.println("✅ Successfully split logs into " + groupedLogs.size() + " monthly files");
    }

    /**
     * Migrate the entire old worklog.json file to the new structure
     */
    public static void migrateOldWorklogFile(Path oldWorklogFile, Path logsDirectory) throws IOException {
        if (!Files.exists(oldWorklogFile)) {
            System.out.println("ℹ️ No old worklog file found at: " + oldWorklogFile);
            return;
        }

        System.out.println("🔄 Starting migration of old worklog file: " + oldWorklogFile);

        try {
            // Read the old format
            WorkLogData oldData = objectMapper.readValue(oldWorklogFile.toFile(), WorkLogData.class);
            List<RegistroTrabalho> allLogs = oldData.getRegistros();

            if (allLogs == null || allLogs.isEmpty()) {
                System.out.println("ℹ️ No work logs found in old file");
                return;
            }

            // Split into monthly files
            splitLogsIntoMonthlyFiles(allLogs, logsDirectory);

            // Backup the old file
            Path backupFile = oldWorklogFile.resolveSibling("worklog-migrated-backup.json");
            Files.move(oldWorklogFile, backupFile);
            System.out.println("📦 Old file backed up to: " + backupFile);

            System.out.println("✅ Migration completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Migration failed: " + e.getMessage());
            throw new IOException("Failed to migrate old worklog file", e);
        }
    }

    /**
     * Auto-detect and fix file format issues
     */
    public static List<RegistroTrabalho> autoFixFileFormat(Path filePath) {
        try {
            return loadWorkLogsWithMigration(filePath);
        } catch (Exception e) {
            System.err.println("❌ Auto-fix failed for " + filePath + ": " + e.getMessage());
            ErrorHandler.handleDataCorruptionError(filePath.getFileName().toString(), e);
            return new ArrayList<>();
        }
    }
}