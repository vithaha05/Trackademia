package com.campus.tracker.controller;

import com.campus.tracker.dao.StudentDAO;
import com.campus.tracker.model.Student;
import com.campus.tracker.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private StudentDAO studentDAO;

    public LoginController() {
        this.studentDAO = new StudentDAO();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        Student student = studentDAO.login(username, password);

        if (student != null) {
            SessionManager.getInstance().setCurrentStudent(student);
            loadDashboard();
        } else {
            showError("Invalid username or password");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double width = Math.min(1400, screenBounds.getWidth() * 0.8);
            double height = Math.min(900, screenBounds.getHeight() * 0.85);

            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setX((screenBounds.getWidth() - width) / 2);
            stage.setY((screenBounds.getHeight() - height) / 2);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load registration page");
        }
    }

    private void loadDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double width = Math.min(1600, screenBounds.getWidth() * 0.9);
            double height = Math.min(1000, screenBounds.getHeight() * 0.9);

            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setTitle("Trackademia - Dashboard");
            stage.setX((screenBounds.getWidth() - width) / 2);
            stage.setY((screenBounds.getHeight() - height) / 2);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load dashboard");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}