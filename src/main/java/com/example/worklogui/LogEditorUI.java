package com.example.worklogui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Helper class to create and show the Log Editor window
 */
public class LogEditorUI {

    private Runnable onCloseCallback;

    /**
     * Set a callback to be executed when the editor is closed
     * @param callback The callback to execute
     */
    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Shows the Log Editor in a new window
     * @param owner The owner window
     */
    public void show(Stage owner) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("log-editor.fxml"));
            Parent root = loader.load();

            // Get the controller
            LogEditorController controller = loader.getController();

            // Load the current work logs
            controller.setRegistros(FileLoader.carregarRegistros(AppConstants.WORKLOG_PATH));

            // Set callback to notify when changes are made
            controller.setOnSaveCallback(() -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            });

            // Create and show the window
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setTitle("Work Log Editor");
            stage.setScene(new Scene(root));

            // Execute callback when window is closed
            stage.setOnHidden(e -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            });

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Show an error alert
            new Alert(Alert.AlertType.ERROR, "Error opening Log Editor: " + e.getMessage())
                    .showAndWait();
        }
    }
}