package com.psgtech.studentportal.controllers;
import javafx.scene.text.Text;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import com.psgtech.studentportal.MainApp;
import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.services.*;
import com.psgtech.studentportal.utils.SessionManager;
import java.util.List;

/**
 * Dashboard Controller
 * Main application interface with tabs for different features
 */
public class DashboardController {

    // Header
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;

    // CGPA Tab
    @FXML private TableView<CGPARecord> cgpaTable;
    @FXML private TableColumn<CGPARecord, Integer> semesterColumn;
    @FXML private TableColumn<CGPARecord, String> gpaColumn;
    @FXML private TableColumn<CGPARecord, String> cgpaColumn;
    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> courseCodeColumn;
    @FXML private TableColumn<Course, String> courseNameColumn;
    @FXML private TableColumn<Course, Integer> creditsColumn;
    @FXML private TableColumn<Course, String> gradeColumn;

    // Internals Tab
    @FXML private TableView<InternalMarksDisplay> internalsTable;
    @FXML private TableColumn<InternalMarksDisplay, String> internalCourseColumn;
    @FXML private TableColumn<InternalMarksDisplay, Double> internalMarksColumn;
    @FXML private TableColumn<InternalMarksDisplay, String> passScoreColumn;
    @FXML private TableColumn<InternalMarksDisplay, String> targetScoreColumn;
    @FXML private Slider targetSlider;
    @FXML private Label targetLabel;

    // ML Analytics Tab
    @FXML private TableView<PerformanceAnalytics> analyticsTable;
    @FXML private TableColumn<PerformanceAnalytics, String> analyticsCourseColumn;
    @FXML private TableColumn<PerformanceAnalytics, Double> predictedScoreColumn;
    @FXML private TableColumn<PerformanceAnalytics, Double> percentileColumn;
    @FXML private TableColumn<PerformanceAnalytics, String> recommendationColumn;
    @FXML private Button refreshAnalyticsButton;
    @FXML private ProgressIndicator analyticsProgress;

    private SessionManager sessionManager;
    private ScraperService scraperService;
    private DatabaseService databaseService;
    private MLPredictionService mlService;

    private List<InternalMarks> currentInternals;
    private int currentSemester = 1;

    @FXML
    public void initialize() {
        System.out.println("📊 Initializing dashboard...");

        sessionManager = MainApp.getSessionManager();
        scraperService = new ScraperService(sessionManager);
        databaseService = new DatabaseService(MainApp.getDatabaseManager());
        mlService = new MLPredictionService(MainApp.getDatabaseManager());

        setupTables();
        loadUserData();
    }

    private void setupTables() {
        // CGPA Table
        semesterColumn.setCellValueFactory(new PropertyValueFactory<>("semester"));

        gpaColumn.setCellValueFactory(cellData -> {
            CGPARecord record = cellData.getValue();
            if (record.isHasBacklogs() || record.getGpa() == null) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(String.format("%.3f", record.getGpa()));
        });

        cgpaColumn.setCellValueFactory(cellData -> {
            CGPARecord record = cellData.getValue();
            if (record.isHasBacklogs() || record.getCgpa() == null) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(String.format("%.3f", record.getCgpa()));
        });

        // Courses Table
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        creditsColumn.setCellValueFactory(new PropertyValueFactory<>("credits"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));

        // Internals Table
        internalCourseColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        internalMarksColumn.setCellValueFactory(new PropertyValueFactory<>("totalMarks"));
        passScoreColumn.setCellValueFactory(new PropertyValueFactory<>("passScore"));
        targetScoreColumn.setCellValueFactory(new PropertyValueFactory<>("targetScore"));

        // Analytics Table
        analyticsCourseColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        predictedScoreColumn.setCellValueFactory(new PropertyValueFactory<>("predictedEndsemScore"));
        percentileColumn.setCellValueFactory(new PropertyValueFactory<>("classPercentile"));
        recommendationColumn.setCellValueFactory(new PropertyValueFactory<>("recommendation"));

        // Make recommendation column wrap text
        recommendationColumn.setCellFactory(tc -> {
            TableCell<PerformanceAnalytics, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(recommendationColumn.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });

        // Target slider listener
        targetSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            targetLabel.setText(String.format("Target: %.0f", newVal.doubleValue()));
            updateInternalsTable(newVal.doubleValue());
        });

        // Hide analytics progress initially
        if (analyticsProgress != null) {
            analyticsProgress.setVisible(false);
        }
    }

    private void loadUserData() {
        System.out.println("📥 Loading user data...");

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Update welcome message
                String studentName = sessionManager.fetchStudentName();
                javafx.application.Platform.runLater(() -> {
                    welcomeLabel.setText("Welcome, " + studentName + "! 👋");
                });

                // Load and save CGPA data
                System.out.println("📚 Loading courses and CGPA...");
                List<Course> courses = scraperService.scrapeCompletedCourses();
                int completedSem = scraperService.getCompletedSemester();
                currentSemester = completedSem;

                // Save courses to database
                for (Course course : courses) {
                    databaseService.saveCourse(course);
                }

                // Calculate and save CGPA
                List<CGPARecord> cgpaRecords = scraperService.calculateCGPA(courses, completedSem);
                for (CGPARecord record : cgpaRecords) {
                    databaseService.saveCGPARecord(record);
                }

                // Update CGPA table on UI thread
                javafx.application.Platform.runLater(() -> {
                    cgpaTable.setItems(FXCollections.observableArrayList(cgpaRecords));
                    coursesTable.setItems(FXCollections.observableArrayList(courses));
                    System.out.println("✅ CGPA data loaded");
                });

                // Load and save internal marks
                System.out.println("📊 Loading internal marks...");
                currentInternals = scraperService.scrapeInternalMarks();

                for (InternalMarks internal : currentInternals) {
                    internal.setSemester(currentSemester);
                    databaseService.saveInternalMarks(internal);
                }

                // Update internals table on UI thread
                javafx.application.Platform.runLater(() -> {
                    updateInternalsTable(50.0);
                    System.out.println("✅ Internal marks loaded");
                });

                System.out.println("✅ All data loaded successfully!");
                return null;
            }
        };

        loadTask.setOnFailed(event -> {
            Throwable exception = loadTask.getException();
            System.err.println("❌ Error loading data: " + exception.getMessage());
            exception.printStackTrace();
            showAlert("Error", "Failed to load data from portal. Please try logging in again.");
        });

        new Thread(loadTask).start();
    }

    private void updateInternalsTable(double targetScore) {
        if (currentInternals == null || currentInternals.isEmpty()) {
            return;
        }

        ObservableList<InternalMarksDisplay> displayList = FXCollections.observableArrayList();

        for (InternalMarks internal : currentInternals) {
            if (internal.getTotalInternalMarks() == null) continue;

            InternalMarksDisplay display = new InternalMarksDisplay();
            display.setCourseName(internal.getCourseCode() + " - " + internal.getCourseName());
            display.setTotalMarks(internal.getTotalInternalMarks());

            // Calculate required scores
            // Formula: final = 0.4 * (internal/50 * 100) + 0.6 * (endsem/100 * 100)
            // Simplified: final = 0.8 * internal + 0.6 * endsem
            // Solving for endsem: endsem = (final - 0.8 * internal) / 0.6

            double passScore = calculateRequiredScore(internal.getTotalInternalMarks(), 50);
            double targetRequired = calculateRequiredScore(internal.getTotalInternalMarks(), targetScore);

            display.setPassScore(passScore >= 0 ? String.format("%.0f", passScore) : "-");
            display.setTargetScore(targetRequired >= 0 ? String.format("%.0f", targetRequired) : "-");

            displayList.add(display);
        }

        internalsTable.setItems(displayList);
    }

    /**
     * Calculate required end-semester score to achieve target final score
     */
    private double calculateRequiredScore(double internalMarks, double targetFinal) {
        // Formula: final = 0.4 * (internal/50 * 100) + 0.6 * (endsem/100 * 100)
        // Simplified: final = 0.8 * internal + 0.6 * endsem
        // Solving for endsem: endsem = (final - 0.8 * internal) / 0.6

        double required = (targetFinal - 0.8 * internalMarks) / 0.6;

        if (required > 100) return -1; // Impossible to achieve
        if (required < 45) return 45;  // Minimum passing marks

        return Math.ceil(required);
    }

    @FXML
    private void handleRefreshAnalytics() {
        System.out.println("🤖 Generating ML analytics...");

        if (currentInternals == null || currentInternals.isEmpty()) {
            showAlert("No Data", "No internal marks available to analyze.");
            return;
        }

        refreshAnalyticsButton.setDisable(true);
        analyticsProgress.setVisible(true);

        Task<List<PerformanceAnalytics>> analyticsTask = new Task<List<PerformanceAnalytics>>() {
            @Override
            protected List<PerformanceAnalytics> call() throws Exception {
                List<PerformanceAnalytics> analyticsList = new java.util.ArrayList<>();

                for (InternalMarks internal : currentInternals) {
                    if (internal.getTotalInternalMarks() == null) continue;

                    PerformanceAnalytics analytics = mlService.generateAnalytics(
                            sessionManager.getRollNo(),
                            internal.getCourseCode(),
                            currentSemester,
                            internal.getTotalInternalMarks()
                    );

                    analyticsList.add(analytics);
                }

                return analyticsList;
            }
        };

        analyticsTask.setOnSucceeded(event -> {
            List<PerformanceAnalytics> analytics = analyticsTask.getValue();
            analyticsTable.setItems(FXCollections.observableArrayList(analytics));
            refreshAnalyticsButton.setDisable(false);
            analyticsProgress.setVisible(false);
            System.out.println("✅ Analytics generated successfully!");
        });

        analyticsTask.setOnFailed(event -> {
            Throwable exception = analyticsTask.getException();
            System.err.println("❌ Error generating analytics: " + exception.getMessage());
            exception.printStackTrace();
            showAlert("Error", "Failed to generate analytics: " + exception.getMessage());
            refreshAnalyticsButton.setDisable(false);
            analyticsProgress.setVisible(false);
        });

        new Thread(analyticsTask).start();
    }

    @FXML
    private void handleLogout() {
        System.out.println("🔓 Logging out...");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will need to login again to access the portal.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sessionManager.clearSession();
                try {
                    MainApp.showLoginScreen();
                    System.out.println("✅ Logged out successfully");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Display class for internal marks table
     */
    public static class InternalMarksDisplay {
        private String courseName;
        private Double totalMarks;
        private String passScore;
        private String targetScore;

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public Double getTotalMarks() { return totalMarks; }
        public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }

        public String getPassScore() { return passScore; }
        public void setPassScore(String passScore) { this.passScore = passScore; }

        public String getTargetScore() { return targetScore; }
        public void setTargetScore(String targetScore) { this.targetScore = targetScore; }
    }
}