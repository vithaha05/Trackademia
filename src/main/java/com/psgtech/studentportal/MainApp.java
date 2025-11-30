package com.psgtech.studentportal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.psgtech.studentportal.database.DatabaseManager;
import com.psgtech.studentportal.utils.SessionManager;

/**
 * Main Application Entry Point
 * Initializes database and launches JavaFX UI
 * Manages application-wide state and navigation
 */
public class MainApp extends Application {

    private static MainApp instance;
    private Stage primaryStage;
    private static SessionManager sessionManager;
    private static DatabaseManager databaseManager;

    @Override
    public void start(Stage primaryStage) {
        try {
            instance = this;
            this.primaryStage = primaryStage;

            // Initialize managers
            databaseManager = DatabaseManager.getInstance();
            databaseManager.initializeDatabase();
            System.out.println("✅ Database initialized successfully!");

            sessionManager = new SessionManager();
            System.out.println("✅ Session Manager initialized!");

            // Show login screen
            showLoginScreen();

            System.out.println("✅ Application started successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error starting application: " + e.getMessage());
        }
    }

    /**
     * Shows the login screen
     */
    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/views/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(MainApp.class.getResource("/styles/style.css").toExternalForm());

            instance.primaryStage.setTitle("Tracademia - PSG Tech Student Portal");
            instance.primaryStage.setScene(scene);
            instance.primaryStage.show();

            System.out.println("✅ Login screen displayed");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error showing login screen: " + e.getMessage());
        }
    }

    /**
     * Shows the dashboard screen
     */
    public static void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/views/DashboardView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/styles/style.css").toExternalForm());

            instance.primaryStage.setTitle("Tracademia - Dashboard");
            instance.primaryStage.setScene(scene);
            instance.primaryStage.show();

            System.out.println("✅ Dashboard displayed");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error showing dashboard: " + e.getMessage());
        }
    }

    /**
     * Gets the singleton instance of MainApp
     */
    public static MainApp getInstance() {
        return instance;
    }

    /**
     * Gets the session manager
     */
    public static SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Gets the database manager
     */
    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Gets the primary stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}