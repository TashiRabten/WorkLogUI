package com.example.worklogui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        UpdateChecker.checkForUpdates();
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/example/worklogui/main-view.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("WorkLog");
        var styleUrl = getClass().getResource("/style.css");
        if (styleUrl != null) {
            scene.getStylesheets().add(styleUrl.toExternalForm());
        } else {
            System.err.println("⚠️ style.css not found in resources!");
        }

        // Make application resizable with minimum size constraints
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        InputStream iconStream = getClass().getResourceAsStream("/Icons/WorkLog.jpg");
        if (iconStream == null) {
            System.err.println("❌ Failed to load WorkLog.jpg icon!");
        } else {
            primaryStage.getIcons().add(new Image(iconStream));
            System.out.println("✅ Icon loaded successfully.");
        }

        primaryStage.setOnCloseRequest(e -> {
            AutoUpdater.shutdown();
            javafx.application.Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        AutoUpdater.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);

    }
}
