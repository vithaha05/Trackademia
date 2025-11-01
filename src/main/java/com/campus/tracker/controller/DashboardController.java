package com.campus.tracker.controller;

import com.campus.tracker.dao.*;
import com.campus.tracker.model.*;
import com.campus.tracker.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label rollNoLabel;

    @FXML
    private Label cgpaLabel;

    @FXML
    private TabPane mainTabPane;

    // Exam Results Table
    @FXML
    private TableView<ExamResultRow> examResultsTable;
    @FXML
    private TableColumn<ExamResultRow, String> colCourseCode;
    @FXML
    private TableColumn<ExamResultRow, String> colCourseTitle;
    @FXML
    private TableColumn<ExamResultRow, Integer> colCredit;
    @FXML
    private TableColumn<ExamResultRow, String> colGrade;
    @FXML
    private TableColumn<ExamResultRow, Double> colGradePoint;

    // CA Marks Table
    @FXML
    private TableView<CAMarksRow> caMarksTable;
    @FXML
    private TableColumn<CAMarksRow, String> colCACode;
    @FXML
    private TableColumn<CAMarksRow, String> colCATitle;
    @FXML
    private TableColumn<CAMarksRow, String> colT1;
    @FXML
    private TableColumn<CAMarksRow, String> colT2;
    @FXML
    private TableColumn<CAMarksRow, String> colTotal;
    @FXML
    private TableColumn<CAMarksRow, String> colConvTotal;

    private StudentDAO studentDAO;
    private SubjectDAO subjectDAO;
    private GradeDAO gradeDAO;
    private CAMarksDAO caMarksDAO;
    private Student currentStudent;

    public DashboardController() {
        this.studentDAO = new StudentDAO();
        this.subjectDAO = new SubjectDAO();
        this.gradeDAO = new GradeDAO();
        this.caMarksDAO = new CAMarksDAO();
    }

    @FXML
    public void initialize() {
        currentStudent = SessionManager.getInstance().getCurrentStudent();

        if (currentStudent != null) {
            setupUI();
            loadExamResults();
            loadCAMarks();
            calculateCGPA();
        }
    }

    private void setupUI() {
        welcomeLabel.setText("Welcome, " + currentStudent.getName());
        rollNoLabel.setText("Roll No: " + currentStudent.getUsername());

        // Setup Exam Results Table
        colCourseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        colCourseTitle.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
        colCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        colGradePoint.setCellValueFactory(new PropertyValueFactory<>("gradePoint"));

        // Setup CA Marks Table
        colCACode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        colCATitle.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
        colT1.setCellValueFactory(new PropertyValueFactory<>("t1"));
        colT2.setCellValueFactory(new PropertyValueFactory<>("t2"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colConvTotal.setCellValueFactory(new PropertyValueFactory<>("convTotal"));
    }

    private void loadCAMarksData() {
        caMarksTable.getItems().clear();

        Student currentStudent = SessionManager.getInstance().getCurrentStudent();
        if (currentStudent == null) return;

        List<Subject> subjects = subjectDAO.getSubjectsByStudent(currentStudent.getId());

        for (Subject subject : subjects) {
            CAMarks caMarks = caMarksDAO.getBySubjectId(subject.getId());
            if (caMarks != null) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(subject.getSubjectCode());
                row.add(subject.getSubjectName());

                // Check subject type based on available data
                String type = caMarks.getType(); // Get type from database

                if ("LAB".equals(type)) {
                    // Lab format: LT1, LT2, Total, Conv.Total
                    row.add(caMarks.getLt1() != null ? caMarks.getLt1() : "-");
                    row.add(caMarks.getLt2() != null ? caMarks.getLt2() : "-");
                    row.add("-"); // Empty T1 for labs
                    row.add("-"); // Empty T2 for labs
                } else {
                    // Theory format: T1, T2, Total, Conv.Total
                    row.add("-"); // Empty LT1 for theory
                    row.add("-"); // Empty LT2 for theory
                    row.add(caMarks.getT1() != null ? caMarks.getT1() : "-");
                    row.add(caMarks.getT2() != null ? caMarks.getT2() : "-");
                }

                row.add(caMarks.getTotal() != null ? caMarks.getTotal() : "-");
                row.add(caMarks.getConvTotal() != null ? caMarks.getConvTotal() : "-");

                caMarksTable.getItems().add(row);
            }
        }
    }

    private void loadCAMarks() {
        ObservableList<CAMarksRow> data = FXCollections.observableArrayList();

        List<Subject> subjects = subjectDAO.getSubjectsByStudent(currentStudent.getId());

        for (Subject subject : subjects) {
            CAMarks ca = caMarksDAO.getBySubjectId(subject.getId());

            if (ca != null) {
                CAMarksRow row = new CAMarksRow(
                        subject.getSubjectCode(),
                        subject.getSubjectName(),
                        ca.getT1(),
                        ca.getT2(),
                        ca.getTotal(),
                        ca.getConvTotal()
                );
                data.add(row);
            }
        }

        caMarksTable.setItems(data);
    }

    private void calculateCGPA() {
        List<Subject> subjects = subjectDAO.getSubjectsByStudent(currentStudent.getId());

        double totalCredits = 0;
        double totalGradePoints = 0;

        for (Subject subject : subjects) {
            Grade grade = gradeDAO.getBySubjectId(subject.getId());

            if (grade != null && grade.getGradePoint() > 0) {
                totalCredits += subject.getCredits();
                totalGradePoints += (grade.getGradePoint() * subject.getCredits());
            }
        }

        double cgpa = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
        cgpaLabel.setText(String.format("CGPA: %.2f", cgpa));
    }

    // Inner classes for TableView rows
    public static class ExamResultRow {
        private String courseCode;
        private String courseTitle;
        private int credit;
        private String grade;
        private double gradePoint;

        public ExamResultRow(String courseCode, String courseTitle, int credit, String grade, double gradePoint) {
            this.courseCode = courseCode;
            this.courseTitle = courseTitle;
            this.credit = credit;
            this.grade = grade;
            this.gradePoint = gradePoint;
        }

        public String getCourseCode() { return courseCode; }
        public String getCourseTitle() { return courseTitle; }
        public int getCredit() { return credit; }
        public String getGrade() { return grade; }
        public double getGradePoint() { return gradePoint; }
    }

    public static class CAMarksRow {
        private String courseCode;
        private String courseTitle;
        private String t1;
        private String t2;
        private String total;
        private String convTotal;

        public CAMarksRow(String courseCode, String courseTitle, String t1, String t2, String total, String convTotal) {
            this.courseCode = courseCode;
            this.courseTitle = courseTitle;
            this.t1 = t1;
            this.t2 = t2;
            this.total = total;
            this.convTotal = convTotal;
        }

        public String getCourseCode() { return courseCode; }
        public String getCourseTitle() { return courseTitle; }
        public String getT1() { return t1; }
        public String getT2() { return t2; }
        public String getTotal() { return total; }
        public String getConvTotal() { return convTotal; }
    }

    @FXML
    private void handleRefresh() {
        loadExamResults();
        loadCAMarks();
        calculateCGPA();
        showAlert("Success", "Data refreshed successfully!");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clearSession();
        // Navigate back to login
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}