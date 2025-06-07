package com.example.worklogui.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Centralized error handling utility
 */
public class ErrorHandler {

    private static Consumer<String> statusMessageHandler;

    /**
     * Set the status message handler for error reporting
     */
    public static void setStatusMessageHandler(Consumer<String> handler) {
        statusMessageHandler = handler;
    }

    /**
     * Handle file operation errors
     */
    public static class FileOperationException extends Exception {
        private final String userMessage;

        public FileOperationException(String userMessage, Throwable cause) {
            super(cause);
            this.userMessage = userMessage;
        }

        public FileOperationException(String userMessage, String detailMessage) {
            super(detailMessage);
            this.userMessage = userMessage;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    /**
     * Handle validation errors - extends RuntimeException so it doesn't need to be caught
     */
    public static class ValidationException extends RuntimeException {
        private final String userMessage;

        public ValidationException(String userMessage) {
            super(userMessage);
            this.userMessage = userMessage;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    /**
     * Log and report file operation error
     */
    public static void handleFileError(String operation, Exception e, String filename) {
        String userMessage = String.format(
                "❌ Error %s file %s\n❌ Erro ao %s arquivo %s",
                operation, filename,
                operation.toLowerCase(), filename
        );

        String detailMessage = String.format("File operation failed: %s on %s - %s",
                operation, filename, e.getMessage());

        System.err.println(detailMessage);
        e.printStackTrace();

        setStatusMessage(userMessage);

        // Show detailed error dialog in development/debug mode
        if (Boolean.getBoolean("worklog.debug")) {
            showErrorDialog("File Operation Error", userMessage, detailMessage);
        }
    }

    /**
     * Handle validation errors
     */
    public static void handleValidationError(String context, String validationMessage) {
        String userMessage = String.format("⚠ %s: %s", context, validationMessage);
        System.err.println("Validation error in " + context + ": " + validationMessage);
        setStatusMessage(userMessage);
    }

    /**
     * Handle unexpected errors
     */
    public static void handleUnexpectedError(String context, Exception e) {
        String userMessage = String.format(
                "❌ Unexpected error in %s\n❌ Erro inesperado em %s",
                context, context
        );

        System.err.println("Unexpected error in " + context + ": " + e.getMessage());
        e.printStackTrace();

        setStatusMessage(userMessage);

        if (Boolean.getBoolean("worklog.debug")) {
            showErrorDialog("Unexpected Error", userMessage, e.getMessage());
        }
    }

    /**
     * Handle network/update errors
     */
    public static void handleNetworkError(String operation, Exception e) {
        String userMessage = String.format(
                "🌐 Network error during %s\n🌐 Erro de rede durante %s",
                operation, operation
        );

        System.err.println("Network error: " + operation + " - " + e.getMessage());
        setStatusMessage(userMessage);
    }

    /**
     * Handle data corruption errors
     */
    public static void handleDataCorruptionError(String filename, Exception e) {
        String userMessage = String.format(
                "💾 Data corruption detected in %s. Creating backup.\n💾 Corrupção de dados detectada em %s. Criando backup.",
                filename, filename
        );

        System.err.println("Data corruption in " + filename + ": " + e.getMessage());
        e.printStackTrace();

        setStatusMessage(userMessage);
        showErrorDialog("Data Corruption", userMessage,
                "The file may be corrupted. A backup will be created and the file will be reset.");
    }

    /**
     * Show error dialog to user
     */
    private static void showErrorDialog(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Show confirmation dialog for potentially destructive operations
     */
    public static boolean showConfirmationDialog(String title, String message) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(title);
        confirmation.setHeaderText(null);
        confirmation.setContentText(message);

        Optional<ButtonType> result = confirmation.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Set status message if handler is available
     */
    private static void setStatusMessage(String message) {
        if (statusMessageHandler != null) {
            statusMessageHandler.accept(message);
        }
    }

    /**
     * Create file operation exception
     */
    public static FileOperationException createFileException(String operation, String filename, Throwable cause) {
        return new FileOperationException(
                String.format("Failed to %s %s", operation.toLowerCase(), filename),
                cause
        );
    }
}