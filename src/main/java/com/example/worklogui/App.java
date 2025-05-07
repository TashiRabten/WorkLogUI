package com.example.worklogui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/example/worklogui/main-view.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("WorkLog");
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/WorkLog.jpg")));
        UpdateChecker.checkForUpdates();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
