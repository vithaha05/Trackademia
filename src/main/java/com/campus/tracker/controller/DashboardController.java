package com.campus.tracker.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {
    @FXML private Label nameLabel, rollNoLabel, programmeLabel, cgpaLabel;
    @FXML private TableView<CourseRow> examTable;  // Renamed for clarity
    @FXML private TableColumn<CourseRow, String> codeCol, titleCol, creditsCol, gradeCol;
    @FXML private ComboBox<String> semesterCombo;  // NEW: Dropdown
    @FXML private TableView<CAMarksRow> caTable;  // Keep separate for CA

    private ObservableList<CourseRow> allCourses = FXCollections.observableArrayList();
    private Map<String, List<CourseRow>> coursesBySem = new HashMap<>();  // NEW: Grouped
    private List<CAMarksRow> caMarksList = new ArrayList<>();

    // ... (keep existing loadExamResults, loadCAMarks methods, but modify loadExamResults)

    private void loadExamResults(List<Map<String, String>> examData) {
        allCourses.clear();
        coursesBySem.clear();

        for (Map<String, String> map : examData) {
            CourseRow row = new CourseRow(
                    map.get("courseCode"),
                    map.get("courseTitle"),
                    map.get("credits"),
                    map.get("grade"),
                    map.get("semester")
            );
            allCourses.add(row);

            String sem = map.get("semester");
            coursesBySem.computeIfAbsent(sem, k -> new ArrayList<>()).add(row);
        }

        // NEW: Populate sorted semesters in ComboBox
        Set<String> semesters = coursesBySem.keySet();
        semesterCombo.getItems().addAll(semesters.stream().sorted().collect(Collectors.toList()));
        semesterCombo.getSelectionModel().selectFirst();  // Default to Sem 1
        updateExamTable(semesterCombo.getValue());  // Load first sem

        // Setup columns
        codeCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
        creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));

        // NEW: Sort table by courseCode
        examTable.getSortOrder().add(codeCol);
        codeCol.setSortable(true);
    }

    // NEW: Listener for semester change
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        semesterCombo.setOnAction(e -> {
            String selectedSem = semesterCombo.getValue();
            if (selectedSem != null) {
                updateExamTable(selectedSem);
            }
        });
        // ... other init
    }

    private void updateExamTable(String semester) {
        List<CourseRow> semCourses = coursesBySem.getOrDefault(semester, Collections.emptyList());
        // Sort by courseCode
        semCourses = semCourses.stream()
                .sorted(Comparator.comparing(CourseRow::getCourseCode))
                .collect(Collectors.toList());
        examTable.setItems(FXCollections.observableArrayList(semCourses));
    }

    // Keep loadCAMarks as-is, but populate caTable with ObservableList

    // Static inner class for rows (keep existing)
    public static class CourseRow {
        private final String courseCode, courseTitle, credits, grade, semester;
        // Constructor, getters...
        public CourseRow(String code, String title, String credits, String grade, String sem) {
            this.courseCode = code; this.courseTitle = title; this.credits = credits;
            this.grade = grade; this.semester = sem;
        }
        // Getters...
        public String getCourseCode() { return courseCode; }
        // ... other getters
    }

    // Keep CAMarksRow...
}