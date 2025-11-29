package com.psgtech.studentportal.controllers;

import com.psgtech.studentportal.database.DatabaseManager;
import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.services.DatabaseService;
import com.psgtech.studentportal.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {  // ✅ Added Initializable

    @FXML private Label userLabel;
    @FXML private Label cgpaLabel;
    @FXML private Label gpaLabel;
    @FXML private Label semesterLabel;
    @FXML private Label coursesLabel;
    @FXML private Button logoutButton;

    private String studentRollNo;
    private DatabaseService databaseService;
    private SessionManager sessionManager;

    @Override  // ✅ FXML-standard initialize (replaces your @FXML initialize())
    public void initialize(URL location, ResourceBundle resources) {
        this.databaseService = new DatabaseService(DatabaseManager.getInstance());
        this.sessionManager = SessionManager.getInstance();

        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        }
        System.out.println("✅ Dashboard controller initialized");
    }

    /**
     * Initialize dashboard with student data
     * Called from LoginController after successful login
     */
    public void initializeWithStudent(String rollNo) {
        this.studentRollNo = rollNo;
        if (userLabel != null) {
            userLabel.setText("User: " + rollNo);
        }
        loadStudentData();
    }

    /**
     * Set student roll number (alternative method name for compatibility)
     */
    public void setStudentRollNo(String rollNo) {
        this.studentRollNo = rollNo;
        if (userLabel != null) {
            userLabel.setText("User: " + rollNo);
        }
    }

    /**
     * Load student data from database
     */
    public void loadStudentData() {
        if (studentRollNo == null) {
            System.err.println("❌ No student roll number set");
            return;
        }

        new Thread(() -> {
            try {
                Student student = databaseService.getStudent(studentRollNo);
                List<CGPARecord> cgpaHistory = databaseService.getCGPAHistory(studentRollNo);
                List<Course> courses = databaseService.getCourses(studentRollNo);
                List<InternalMarks> internalMarks = databaseService.getInternalMarks(studentRollNo);

                Platform.runLater(() -> {
                    displayStudentInfo(student);
                    displayCGPAInfo(cgpaHistory);
                    displayCourses(courses);
                    System.out.println("✅ Dashboard loaded for student: " + studentRollNo);
                });

            } catch (SQLException e) {
                System.err.println("❌ Error loading student data: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> loadPlaceholderData());
            }
        }).start();
    }

    private void displayStudentInfo(Student student) {
        if (student != null && userLabel != null) {
            String displayName = student.getName() != null ? student.getName() : student.getRollNo();
            userLabel.setText("Welcome, " + displayName + "!");
            if (semesterLabel != null) {
                semesterLabel.setText("Semester: " + student.getCurrentSemester());
            }
        }
    }

    private void displayCGPAInfo(List<CGPARecord> cgpaHistory) {
        if (cgpaHistory.isEmpty()) {
            if (cgpaLabel != null) cgpaLabel.setText("CGPA: --");
            if (gpaLabel != null) gpaLabel.setText("GPA: --");
            return;
        }

        CGPARecord latestRecord = cgpaHistory.get(cgpaHistory.size() - 1);
        if (cgpaLabel != null) {
            String cgpaText = latestRecord.getCgpa() != null
                    ? String.format("%.2f", latestRecord.getCgpa())
                    : "--";
            cgpaLabel.setText("CGPA: " + cgpaText);
        }
        if (gpaLabel != null) {
            String gpaText = latestRecord.getGpa() != null
                    ? String.format("%.2f", latestRecord.getGpa())
                    : "--";
            gpaLabel.setText("GPA: " + gpaText);
        }
    }

    private void displayCourses(List<Course> courses) {
        if (coursesLabel == null) return;
        if (courses.isEmpty()) {
            coursesLabel.setText("No courses found");
            return;
        }

        int currentSemester = courses.stream()
                .mapToInt(Course::getSemester)
                .max()
                .orElse(1);

        StringBuilder coursesText = new StringBuilder("Current Semester Courses:\n\n");
        for (Course course : courses) {
            if (course.getSemester() == currentSemester) {
                coursesText.append("• ")
                        .append(course.getCourseName())
                        .append(" (")
                        .append(course.getCourseCode())
                        .append(")")
                        .append(" - Grade: ")
                        .append(course.getGrade() != null ? course.getGrade() : "Pending")
                        .append("\n");
            }
        }
        coursesLabel.setText(coursesText.toString());
    }

    private void loadPlaceholderData() {
        System.out.println("⚠️ Loading placeholder data");
        if (userLabel != null) userLabel.setText("User: " + studentRollNo);
        if (cgpaLabel != null) cgpaLabel.setText("CGPA: --");
        if (gpaLabel != null) gpaLabel.setText("GPA: --");
        if (semesterLabel != null) semesterLabel.setText("Semester: --");
        if (coursesLabel != null) coursesLabel.setText("Loading courses...\nPlease wait for data to sync.");
    }

    private void handleLogout() {
        System.out.println("🔄 Logging out...");
        sessionManager.logout();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene loginScene = new Scene(loginRoot);

            try {
                String css = getClass().getResource("/css/style.css").toExternalForm();
                loginScene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ Could not load CSS: " + e.getMessage());
            }

            stage.setScene(loginScene);
            stage.setTitle("Tracademia - Login");
            System.out.println("✅ User logged out");
            System.out.println("✅ Login screen displayed");

        } catch (IOException e) {
            System.err.println("❌ Failed to load login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
