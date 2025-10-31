package com.campus.tracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double width = Math.min(1400, screenBounds.getWidth() * 0.8);
            double height = Math.min(900, screenBounds.getHeight() * 0.85);

            Scene scene = new Scene(root, width, height);
            primaryStage.setTitle("Trackademia - Login");
            primaryStage.setScene(scene);
            primaryStage.setX((screenBounds.getWidth() - width) / 2);
            primaryStage.setY((screenBounds.getHeight() - height) / 2);
            primaryStage.show();

            System.out.println("Window size: " + width + "x" + height);
            System.out.println("Screen size: " + screenBounds.getWidth() + "x" + screenBounds.getHeight());

        } catch (Exception e) {
            System.err.println("Error loading FXML:");
            e.printStackTrace();

            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
                    "Error loading login screen. Check console for details.");
            javafx.scene.layout.StackPane errorPane = new javafx.scene.layout.StackPane(errorLabel);
            primaryStage.setScene(new Scene(errorPane, 600, 400));
            primaryStage.show();
        }
    }

    @Override
    public void stop() {
        com.campus.tracker.util.DatabaseConfig.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}