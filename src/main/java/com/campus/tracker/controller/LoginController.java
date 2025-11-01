package com.campus.tracker.controller;

import com.campus.tracker.dao.*;
import com.campus.tracker.model.*;
import com.campus.tracker.util.ECampusManager;
import com.campus.tracker.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label loadingLabel;

    @FXML
    private Button loginButton;

    private StudentDAO studentDAO;
    private SubjectDAO subjectDAO;
    private GradeDAO gradeDAO;
    private CAMarksDAO caMarksDAO;

    public LoginController() {
        this.studentDAO = new StudentDAO();
        this.subjectDAO = new SubjectDAO();
        this.gradeDAO = new GradeDAO();
        this.caMarksDAO = new CAMarksDAO();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter PSG Tech Roll No & eCampus Password");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Connecting...");
        showLoading("Connecting to eCampus...");

        new Thread(() -> {
            try {
                // Use ECampusManager to fetch all data
                ECampusManager manager = new ECampusManager();

                Platform.runLater(() -> showLoading("Scraping exam results & CA marks..."));
                Map<String, Object> data = manager.fetchAllData(username, password);

                // Save to database
                Platform.runLater(() -> showLoading("Saving data to database..."));
                saveScrapedData(username, data);

                // Load dashboard
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                    hideLoading();
                    loadDashboard();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                    hideLoading();
                    showError("Login failed: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void saveScrapedData(String rollNo, Map<String, Object> data) {
        try {
            // Check if student exists
            Student student = studentDAO.getByUsername(rollNo);

            if (student == null) {
                // Create new student
                String name = (String) data.get("name");
                if (name == null || name.isEmpty()) {
                    name = rollNo;
                }
                studentDAO.register(rollNo, "temp", name, rollNo + "@psgtech.ac.in");
                student = studentDAO.getByUsername(rollNo);
            }

            SessionManager.getInstance().setCurrentStudent(student);

            // Save Exam Results (CGPA data)
            @SuppressWarnings("unchecked")
            List<Map<String, String>> examResults = (List<Map<String, String>>) data.get("examResults");

            if (examResults != null && !examResults.isEmpty()) {
                for (Map<String, String> result : examResults) {
                    String courseCode = result.get("courseCode");
                    String courseTitle = result.get("courseTitle");
                    String creditStr = result.get("credit");
                    String gradeStr = result.get("grade");
                    String resultStatus = result.get("result");

                    // Parse credits
                    int credits = 4;
                    try {
                        credits = Integer.parseInt(creditStr);
                    } catch (Exception e) {
                        // use default
                    }

                    // Find or create subject
                    Subject subject = findOrCreateSubject(student.getId(), courseCode, courseTitle, credits);

                    // Add grade if Pass
                    if (resultStatus != null && resultStatus.equalsIgnoreCase("Pass")) {
                        double gradePoint = convertGradeToPoint(gradeStr);

                        Grade existingGrade = gradeDAO.getBySubjectId(subject.getId());
                        if (existingGrade != null) {
                            existingGrade.setGradePoint(gradePoint);
                            existingGrade.setGrade(gradeStr);
                            gradeDAO.updateGrade(existingGrade);
                        } else {
                            Grade grade = new Grade(
                                    subject.getId(),
                                    0,
                                    0,
                                    0,
                                    gradePoint,
                                    gradeStr
                            );
                            gradeDAO.addGrade(grade);
                        }
                    }
                }
            }

            // Save CA Marks
            @SuppressWarnings("unchecked")
            List<Map<String, String>> caMarks = (List<Map<String, String>>) data.get("caMarks");

            if (caMarks != null && !caMarks.isEmpty()) {
                for (Map<String, String> ca : caMarks) {
                    String courseCode = ca.get("courseCode");
                    String courseTitle = ca.get("courseTitle");

                    // Find or create subject
                    Subject subject = findOrCreateSubject(student.getId(), courseCode, courseTitle, 4);

                    // Save CA marks
                    CAMarks existingCA = caMarksDAO.getBySubjectId(subject.getId());

                    CAMarks caMarksObj = new CAMarks();
                    caMarksObj.setSubjectId(subject.getId());
                    caMarksObj.setT1(ca.getOrDefault("t1", ""));
                    caMarksObj.setT2(ca.getOrDefault("t2", ""));
                    caMarksObj.setRt(ca.getOrDefault("rt", ""));
                    caMarksObj.setRt1(ca.getOrDefault("rt1", ""));
                    caMarksObj.setRt2(ca.getOrDefault("rt2", ""));
                    caMarksObj.setAp(ca.getOrDefault("ap", ""));
                    caMarksObj.setMp1(ca.getOrDefault("mp1", ""));
                    caMarksObj.setMp2(ca.getOrDefault("mp2", ""));
                    caMarksObj.setTotal(ca.getOrDefault("total", ""));
                    caMarksObj.setConvTotal(ca.getOrDefault("convTotal", ""));

                    if (existingCA != null) {
                        caMarksObj.setId(existingCA.getId());
                        caMarksDAO.updateCAMarks(caMarksObj);
                    } else {
                        caMarksDAO.addCAMarks(caMarksObj);
                    }
                }
            }

            System.out.println("Data saved successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save data: " + e.getMessage());
        }
    }

    private Subject findOrCreateSubject(int studentId, String courseCode, String courseTitle, int credits) {
        List<Subject> subjects = subjectDAO.getSubjectsByStudent(studentId);

        // Try to find existing subject by course code
        for (Subject s : subjects) {
            if (s.getSubjectCode().equalsIgnoreCase(courseCode)) {
                return s;
            }
        }

        // Create new subject if not found
        Subject newSubject = new Subject(studentId, courseCode, courseTitle, credits, 1);
        subjectDAO.addSubject(newSubject);

        // Fetch it back to get the generated ID
        subjects = subjectDAO.getSubjectsByStudent(studentId);
        for (Subject s : subjects) {
            if (s.getSubjectCode().equalsIgnoreCase(courseCode)) {
                return s;
            }
        }

        return newSubject;
    }

    private double convertGradeToPoint(String grade) {
        if (grade == null || grade.isEmpty() || grade.equals("-")) {
            return 0.0;
        }

        // Extract just the letter grade (remove number like "6 B")
        String letterGrade = grade.replaceAll("[0-9]", "").trim();

        switch (letterGrade.toUpperCase()) {
            case "O": return 10.0;
            case "A+": return 9.0;
            case "A": return 8.0;
            case "B+": return 7.0;
            case "B": return 6.0;
            case "C": return 5.0;
            case "D": return 4.0;
            default: return 0.0;
        }
    }

    @FXML
    private void handleRegister() {
        showError("Use your PSG Tech eCampus credentials to login");
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

    private void showLoading(String message) {
        if (loadingLabel != null) {
            loadingLabel.setText(message);
            loadingLabel.setVisible(true);
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    private void hideLoading() {
        if (loadingLabel != null) {
            loadingLabel.setVisible(false);
        }
    }

    private void showError(String message) {
        hideLoading();
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}