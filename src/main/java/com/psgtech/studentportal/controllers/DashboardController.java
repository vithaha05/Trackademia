package com.psgtech.studentportal.controllers;

import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.services.DatabaseService;
import com.psgtech.studentportal.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblWelcome;
    @FXML private Label lblCGPA;
    @FXML private Label lblCurrentGPA;
    @FXML private Label lblCurrentSemester;
    @FXML private ComboBox<String> cmbSemesterSelector;
    @FXML private TableView<Course> tblCourses;
    @FXML private TableColumn<Course, String> colCourseCode;
    @FXML private TableColumn<Course, String> colCourseName;
    @FXML private TableColumn<Course, String> colCategory;
    @FXML private TableColumn<Course, Integer> colCredits;
    @FXML private TableColumn<Course, String> colGrade;
    @FXML private TableColumn<Course, Double> colGradePoints;
    @FXML private Button btnLogout;

    private DatabaseService databaseService;
    private SessionManager sessionManager;
    private String rollNo;
    private List<Course> allCourses;
    private Map<Integer, CGPARecord> cgpaRecords;

    public void initialize() {
        System.out.println("✅ Dashboard controller initialized");
        tblCourses.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sessionManager = SessionManager.getInstance();
        rollNo = sessionManager.getLoggedInStudentRollNo();

        if (rollNo == null) {
            showError("No user logged in!");
            return;
        }

        // Setup table columns first
        setupTableColumns();
    }

    /**
     * Initialize dashboard with specific student
     * Called from LoginController after successful login
     */
    public void initializeWithStudent(String studentRollNo) {
        this.rollNo = studentRollNo;
        System.out.println("✅ Dashboard initializing for student: " + rollNo);

        // Setup table columns
        setupTableColumns();

        // Load dashboard data
        loadDashboardData();

        // Setup semester selector listener
        cmbSemesterSelector.setOnAction(e -> handleSemesterChange());

        System.out.println("✅ Dashboard loaded for student: " + rollNo);
    }

    private void setupTableColumns() {
        colCourseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        colCourseName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colCategory.setCellValueFactory(cellData -> {
            String code = cellData.getValue().getCourseCode();
            return new javafx.beans.property.SimpleStringProperty(extractCategory(code));
        });
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        colGradePoints.setCellValueFactory(new PropertyValueFactory<>("gradePoints"));
    }

    private String extractCategory(String courseCode) {
        if (courseCode.length() >= 6) {
            String lastTwo = courseCode.substring(4, 6);
            try {
                int num = Integer.parseInt(lastTwo);
                if (num >= 6 && num <= 9) return "Lab";
                return "Theory";
            } catch (NumberFormatException e) {
                return "Unknown";
            }
        }
        return "Unknown";
    }

    private void loadDashboardData() {
        try {
            databaseService = new DatabaseService(
                    com.psgtech.studentportal.database.DatabaseManager.getInstance()
            );

            // Load student info
            Student student = databaseService.getStudent(rollNo);
            if (student != null) {
                lblWelcome.setText("Welcome, " + student.getName() + "!");
                lblCurrentSemester.setText(String.valueOf(student.getCurrentSemester()));
            }

            // Load all courses
            allCourses = databaseService.getCourses(rollNo);
            System.out.println("📚 Loaded " + allCourses.size() + " courses from database");

            // Get unique semesters and sort
            Set<Integer> semesters = allCourses.stream()
                    .map(Course::getSemester)
                    .collect(Collectors.toSet());

            List<Integer> sortedSemesters = new ArrayList<>(semesters);
            Collections.sort(sortedSemesters);

            // Populate semester dropdown
            ObservableList<String> semesterOptions = FXCollections.observableArrayList();
            semesterOptions.add("All Semesters");
            for (Integer sem : sortedSemesters) {
                semesterOptions.add("Semester " + sem);
            }
            cmbSemesterSelector.setItems(semesterOptions);
            cmbSemesterSelector.setValue("All Semesters");

            // Load CGPA records
            loadCGPARecords();

            // Display all courses initially
            displayAllCourses();

            System.out.println("✅ Dashboard loaded for " + rollNo);

        } catch (SQLException e) {
            System.err.println("❌ Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void loadCGPARecords() throws SQLException {
        cgpaRecords = new HashMap<>();

        List<Course> courses = databaseService.getCourses(rollNo);
        Map<Integer, List<Course>> coursesBySemester = courses.stream()
                .collect(Collectors.groupingBy(Course::getSemester));

        double cumulativeGradePoints = 0;
        int cumulativeCredits = 0;

        for (int sem = 1; sem <= Collections.max(coursesBySemester.keySet()); sem++) {
            List<Course> semCourses = coursesBySemester.get(sem);
            if (semCourses != null && !semCourses.isEmpty()) {
                double semGradePoints = 0;
                int semCredits = 0;

                for (Course course : semCourses) {
                    semGradePoints += course.getGradePoints() * course.getCredits();
                    semCredits += course.getCredits();
                }

                cumulativeGradePoints += semGradePoints;
                cumulativeCredits += semCredits;

                double gpa = semCredits > 0 ? semGradePoints / semCredits : 0;
                double cgpa = cumulativeCredits > 0 ? cumulativeGradePoints / cumulativeCredits : 0;

                CGPARecord record = new CGPARecord();
                record.setSemester(sem);
                record.setGpa(gpa);
                record.setCgpa(cgpa);
                record.setTotalCredits(cumulativeCredits);

                cgpaRecords.put(sem, record);
            }
        }

        // Display overall CGPA
        if (!cgpaRecords.isEmpty()) {
            int maxSem = Collections.max(cgpaRecords.keySet());
            CGPARecord latest = cgpaRecords.get(maxSem);
            lblCGPA.setText(String.format("%.2f", latest.getCgpa()));
            lblCurrentGPA.setText(String.format("%.2f", latest.getGpa()));
        } else {
            lblCGPA.setText("0.00");
            lblCurrentGPA.setText("0.00");
        }
    }

    private void displayAllCourses() {
        ObservableList<Course> courseList = FXCollections.observableArrayList(allCourses);
        courseList.sort((c1, c2) -> Integer.compare(c2.getSemester(), c1.getSemester()));
        tblCourses.setItems(courseList);
        System.out.println("📋 Displaying all " + courseList.size() + " courses");
    }

    private void handleSemesterChange() {
        String selected = cmbSemesterSelector.getValue();

        if (selected == null || selected.equals("All Semesters")) {
            displayAllCourses();

            if (!cgpaRecords.isEmpty()) {
                int maxSem = Collections.max(cgpaRecords.keySet());
                CGPARecord latest = cgpaRecords.get(maxSem);
                lblCGPA.setText(String.format("%.2f", latest.getCgpa()));
                lblCurrentGPA.setText(String.format("%.2f", latest.getGpa()));
            }

        } else {
            int semester = Integer.parseInt(selected.replace("Semester ", ""));

            List<Course> semesterCourses = allCourses.stream()
                    .filter(c -> c.getSemester() == semester)
                    .collect(Collectors.toList());

            ObservableList<Course> courseList = FXCollections.observableArrayList(semesterCourses);
            tblCourses.setItems(courseList);

            CGPARecord record = cgpaRecords.get(semester);
            if (record != null) {
                lblCurrentGPA.setText(String.format("%.2f", record.getGpa()));
                lblCGPA.setText(String.format("%.2f", record.getCgpa()));
            } else {
                lblCurrentGPA.setText("0.00");
                lblCGPA.setText("0.00");
            }

            System.out.println("📋 Displaying " + semesterCourses.size() + " courses for Semester " + semester);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            System.out.println("🔄 Logging out...");
            sessionManager.logout();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogout.getScene().getWindow();
            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Tracademia - PSG Tech Student Portal");

            System.out.println("✅ User logged out");
            System.out.println("✅ Login screen displayed");

        } catch (IOException e) {
            System.err.println("❌ Error loading login screen: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to logout: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}