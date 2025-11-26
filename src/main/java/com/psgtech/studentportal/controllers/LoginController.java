package com.psgtech.studentportal.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.concurrent.Task;
import com.psgtech.studentportal.MainApp;
import com.psgtech.studentportal.utils.SessionManager;

/**
 * Login Controller
 * Handles user authentication
 */
public class LoginController {

    @FXML private TextField rollNoField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private SessionManager sessionManager;

    @FXML
    public void initialize() {
        sessionManager = MainApp.getSessionManager();
        progressIndicator.setVisible(false);
        statusLabel.setText("");

        System.out.println("✅ Login screen initialized");
    }

    @FXML
    private void handleLogin() {
        String rollNo = rollNoField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (rollNo.isEmpty() || password.isEmpty()) {
            statusLabel.setText("❌ Please fill all fields!");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            return;
        }

        // Disable login button and show progress
        loginButton.setDisable(true);
        rollNoField.setDisable(true);
        passwordField.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("🔐 Logging in...");
        statusLabel.setStyle("-fx-text-fill: #1976D2;");

        System.out.println("🔐 Attempting login for: " + rollNo);

        // Perform login in background thread
        Task<Boolean> loginTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Login to both portals
                boolean studzone1Success = sessionManager.loginToStudzone1(rollNo, password);

                if (!studzone1Success) {
                    return false;
                }

                // If studzone1 succeeds, login to studzone2
                boolean studzone2Success = sessionManager.loginToStudzone2(rollNo, password);

                return studzone1Success && studzone2Success;
            }
        };

        loginTask.setOnSucceeded(event -> {
            Boolean success = loginTask.getValue();

            if (success) {
                statusLabel.setText("✅ Login successful! Loading dashboard...");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");

                System.out.println("✅ Login successful for: " + rollNo);

                // Small delay to show success message
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Navigate to dashboard
                try {
                    MainApp.showDashboard();
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("❌ Error loading dashboard!");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                    enableLoginForm();
                }
            } else {
                System.err.println("❌ Login failed for: " + rollNo);
                statusLabel.setText("❌ Invalid credentials! Please try again.");
                statusLabel.setStyle("-fx-text-fill: #F44336;");
                enableLoginForm();
            }
        });

        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            System.err.println("❌ Login error: " + exception.getMessage());
            exception.printStackTrace();

            statusLabel.setText("❌ Connection error! Check your internet.");
            statusLabel.setStyle("-fx-text-fill: #F44336;");
            enableLoginForm();
        });

        // Start the task in a new thread
        new Thread(loginTask).start();
    }

    /**
     * Enable login form after error
     */
    private void enableLoginForm() {
        loginButton.setDisable(false);
        rollNoField.setDisable(false);
        passwordField.setDisable(false);
        progressIndicator.setVisible(false);
    }

    @FXML
    private void handleKeyPress(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleLogin();
        }
    }
}