package com.example.worklogui.utils;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Simple progress dialog for showing task progress
 */
public class ProgressDialog extends Stage {
    
    private final ProgressBar progressBar;
    private final Label messageLabel;
    private final Task<?> task;
    
    public ProgressDialog(Task<?> task) {
        this.task = task;
        
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setResizable(false);
        
        // Create UI components
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.progressProperty().bind(task.progressProperty());
        
        messageLabel = new Label();
        messageLabel.textProperty().bind(task.messageProperty());
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            task.cancel();
            close();
        });
        
        // Layout
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(messageLabel, progressBar, cancelButton);
        
        Scene scene = new Scene(vbox);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        setScene(scene);
        
        // Close dialog when task finishes
        task.setOnSucceeded(e -> close());
        task.setOnFailed(e -> close());
        task.setOnCancelled(e -> close());
    }
    
    public void setHeaderText(String text) {
        setTitle(text);
    }
}