package com.example.worklogui.utils;

import com.example.worklogui.AppConstants;
import com.example.worklogui.RegistroTrabalho;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for file operations with error handling and backup functionality
 */
public class FileOperationHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Path LOGS_DIR = Paths.get(System.getProperty("user.home"), "Documents", "WorkLog", "logs");
    private static final Path BACKUP_DIR = Paths.get(System.getProperty("user.home"), "Documents", "WorkLog", "backups");

    /**
     * Initialize required directories
     */
    public static void initializeDirectories() throws ErrorHandler.FileOperationException {
        try {
            Files.createDirectories(LOGS_DIR);
            Files.createDirectories(BACKUP_DIR);
            Files.createDirectories(AppConstants.EXPORT_FOLDER);
        } catch (IOException e) {
            throw ErrorHandler.createFileException("create", "required directories", e);
        }
    }

    /**
     * Get the path for a log file based on year-month key
     */
    public static Path getLogFilePath(String yearMonthKey) {
        return LOGS_DIR.resolve(yearMonthKey + ".json");
    }

    /**
     * Check if log file exists for given year-month
     */
    public static boolean logFileExists(String yearMonthKey) {
        return Files.exists(getLogFilePath(yearMonthKey));
    }

    /**
     * Load work logs for a specific year-month
     */
    public static List<RegistroTrabalho> loadWorkLogs(String yearMonthKey) throws ErrorHandler.FileOperationException {
        Path logPath = getLogFilePath(yearMonthKey);

        if (!Files.exists(logPath)) {
            return new ArrayList<>();
        }

        try {
            System.out.println("Loading work logs from: " + logPath);

            // Use the migration utility to handle both old and new formats
            List<RegistroTrabalho> logs = FileMigrationUtility.loadWorkLogsWithMigration(logPath);

            System.out.println("Loaded " + logs.size() + " work logs for " + yearMonthKey);
            return logs;
        } catch (ErrorHandler.FileOperationException e) {
            throw e; // Re-throw FileOperationException as-is
        } catch (Exception e) {
            throw ErrorHandler.createFileException("load", logPath.getFileName().toString(), e);
        }
    }

    /**
     * Save work logs for a specific year-month
     */
    public static void saveWorkLogs(String yearMonthKey, List<RegistroTrabalho> logs) throws ErrorHandler.FileOperationException {
        Path logPath = getLogFilePath(yearMonthKey);

        try {
            Files.createDirectories(logPath.getParent());

            if (logs.isEmpty()) {
                // Delete file if no logs
                if (Files.exists(logPath)) {
                    System.out.println("Deleting empty log file: " + logPath);
                    Files.delete(logPath);
                }
                return;
            }

            System.out.println("Saving " + logs.size() + " work logs to " + yearMonthKey);

            // Create backup before saving
            createBackupIfExists(logPath);

            // Save with atomic operation
            Path tempFile = logPath.resolveSibling(logPath.getFileName() + ".tmp");
            objectMapper.writeValue(tempFile.toFile(), logs);
            Files.move(tempFile, logPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("Successfully saved work logs to: " + logPath);

            // Verify the file was created correctly
            verifyFile(logPath, logs.size());

        } catch (IOException e) {
            throw ErrorHandler.createFileException("save", logPath.getFileName().toString(), e);
        }
    }

    /**
     * Get all available year-month keys from log files
     */
    public static List<String> getAvailableYearMonthKeys() throws ErrorHandler.FileOperationException {
        List<String> keys = new ArrayList<>();

        if (!Files.exists(LOGS_DIR)) {
            return keys;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(LOGS_DIR, "*.json")) {
            for (Path path : stream) {
                String filename = path.getFileName().toString();
                String key = filename.replace(".json", "");
                if (key.matches("\\d{4}-\\d{2}")) {  // Validate format YYYY-MM
                    keys.add(key);
                }
            }
        } catch (IOException e) {
            throw ErrorHandler.createFileException("list", "log directory", e);
        }

        keys.sort(String::compareTo);
        return keys;
    }

    /**
     * Load all work logs from all files
     */
    public static List<RegistroTrabalho> loadAllWorkLogs() throws ErrorHandler.FileOperationException {
        List<RegistroTrabalho> allLogs = new ArrayList<>();
        List<String> keys = getAvailableYearMonthKeys();

        for (String key : keys) {
            List<RegistroTrabalho> monthLogs = loadWorkLogs(key);
            allLogs.addAll(monthLogs);
        }

        return allLogs;
    }

    /**
     * Create backup of existing file
     */
    private static void createBackupIfExists(Path originalFile) {
        if (!Files.exists(originalFile)) {
            return;
        }

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = originalFile.getFileName().toString();
            String backupName = filename.replace(".json", "_backup_" + timestamp + ".json");
            Path backupPath = BACKUP_DIR.resolve(backupName);

            Files.createDirectories(BACKUP_DIR);
            Files.copy(originalFile, backupPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Created backup: " + backupPath);
        } catch (IOException e) {
            System.err.println("Warning: Could not create backup for " + originalFile + ": " + e.getMessage());
        }
    }

    /**
     * Verify file was saved correctly
     */
    private static void verifyFile(Path filePath, int expectedCount) throws ErrorHandler.FileOperationException {
        if (!Files.exists(filePath)) {
            throw ErrorHandler.createFileException("verify", filePath.getFileName().toString(),
                    new IOException("File was not created"));
        }

        try {
            long fileSize = Files.size(filePath);
            if (fileSize < 10) {  // File should have some content
                throw ErrorHandler.createFileException("verify", filePath.getFileName().toString(),
                        new IOException("File is too small: " + fileSize + " bytes"));
            }

            // Verify we can read it back
            List<RegistroTrabalho> verifyLogs = objectMapper.readValue(
                    filePath.toFile(),
                    new TypeReference<List<RegistroTrabalho>>() {}
            );

            if (verifyLogs.size() != expectedCount) {
                throw ErrorHandler.createFileException("verify", filePath.getFileName().toString(),
                        new IOException("Expected " + expectedCount + " logs but found " + verifyLogs.size()));
            }

            System.out.println("File verification successful: " + filePath);

        } catch (IOException e) {
            throw ErrorHandler.createFileException("verify", filePath.getFileName().toString(), e);
        }
    }

    /**
     * Clean up old backup files (keep only recent ones)
     */
    public static void cleanupOldBackups(int maxBackupsToKeep) {
        if (!Files.exists(BACKUP_DIR)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BACKUP_DIR, "*_backup_*.json")) {
            List<Path> backupFiles = new ArrayList<>();
            for (Path path : stream) {
                backupFiles.add(path);
            }

            // Sort by last modified time (newest first)
            backupFiles.sort((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (IOException e) {
                    return 0;
                }
            });

            // Delete old backups
            for (int i = maxBackupsToKeep; i < backupFiles.size(); i++) {
                try {
                    Files.delete(backupFiles.get(i));
                    System.out.println("Deleted old backup: " + backupFiles.get(i));
                } catch (IOException e) {
                    System.err.println("Could not delete old backup: " + backupFiles.get(i));
                }
            }

        } catch (IOException e) {
            System.err.println("Error cleaning up backups: " + e.getMessage());
        }
    }

    /**
     * Get logs directory path
     */
    public static Path getLogsDirectory() {
        return LOGS_DIR;
    }

    /**
     * Get backup directory path
     */
    public static Path getBackupDirectory() {
        return BACKUP_DIR;
    }
}