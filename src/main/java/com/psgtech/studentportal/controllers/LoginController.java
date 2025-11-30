package com.psgtech.studentportal.controllers;

import com.psgtech.studentportal.database.DatabaseManager;
import com.psgtech.studentportal.services.ScraperService;
import com.psgtech.studentportal.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField rollNoField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    @FXML
    private Button loginButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    private DatabaseManager databaseManager;
    private SessionManager sessionManager;

    /**
     * Initialize method called after FXML is loaded
     */
    @FXML
    public void initialize() {
        // Hide loading indicator initially
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        // Get singleton instances
        this.databaseManager = DatabaseManager.getInstance();
        this.sessionManager = SessionManager.getInstance();

        // Set up login button action
        if (loginButton != null) {
            loginButton.setOnAction(event -> handleLogin());
        }

        // Allow Enter key to trigger login
        if (passwordField != null) {
            passwordField.setOnAction(event -> handleLogin());
        }

        System.out.println("✅ Login controller initialized");
    }

    /**
     * Handle login button click
     */
    @FXML
    private void handleLogin() {
        String rollNo = rollNoField.getText().trim();
        String password = passwordField.getText().trim();

        // Validation
        if (rollNo.isEmpty() || password.isEmpty()) {
            statusLabel.setText("❌ Please enter both roll number and password");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Show loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        loginButton.setDisable(true);
        statusLabel.setText("🔄 Logging in and fetching data...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        // Run scraping in background thread to avoid freezing UI
        new Thread(() -> {
            try {
                // Create scraper and fetch data from portal
                ScraperService scraper = new ScraperService(databaseManager);
                boolean scraped = scraper.loginAndFetchData(rollNo, password);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    loginButton.setDisable(false);

                    if (scraped) {
                        System.out.println("✅ Data scraped and saved to database");
                        statusLabel.setText("✅ Login successful!");
                        statusLabel.setStyle("-fx-text-fill: green;");

                        // Save session using setLoggedInStudent (not login)
                        sessionManager.setLoggedInStudent(rollNo);

                        // Now load dashboard with fresh data from database
                        showDashboard(rollNo);
                    } else {
                        System.err.println("❌ Could not scrape data from portal");
                        statusLabel.setText("❌ Invalid credentials or network error");
                        statusLabel.setStyle("-fx-text-fill: red;");
                    }
                });

            } catch (Exception e) {
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    loginButton.setDisable(false);
                    statusLabel.setText("❌ Login failed: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    e.printStackTrace();
                });
            }
        }).start();
    }

    /**
     * Show dashboard after successful login
     */
    @FXML
    private void showDashboard(String rollNo) {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();

            // Load DashboardView.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DashboardView.fxml"));
            Parent dashboardRoot = loader.load();

            DashboardController controller = loader.getController();
            controller.initializeWithStudent(rollNo);

            Scene scene = new Scene(dashboardRoot, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Tracademia - Dashboard (" + rollNo + ")");
            stage.show();

            System.out.println("✅ Dashboard loaded for " + rollNo);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Dashboard error: " + e.getMessage());
        }
    }



}