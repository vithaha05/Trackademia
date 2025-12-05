package com.psgtech.studentportal.controllers;

import com.psgtech.studentportal.database.DatabaseManager;
import com.psgtech.studentportal.models.Student;
import com.psgtech.studentportal.services.CGPAPredictionService;
import com.psgtech.studentportal.services.CGPAPredictionService.PredictionDetails;
import com.psgtech.studentportal.services.DatabaseService;
import com.psgtech.studentportal.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PredictionController {

    @FXML
    private Label lblProgram;
    @FXML
    private Label lblPredictedCGPA;
    @FXML
    private Label lblConfidence;
    @FXML
    private Label lblTrend;
    @FXML
    private Label lblProgress;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private LineChart<Number, Number> gpaChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private Label lblPolyPrediction;
    @FXML
    private Label lblWeightedPrediction;
    @FXML
    private FlowPane semesterCardsPane;

    private String rollNo;
    private SessionManager sessionManager;

    @FXML
    public void initialize() {
        System.out.println("✅ Prediction controller initialized");
        sessionManager = SessionManager.getInstance();
    }

    public void initializeWithStudent(String studentRollNo) {
        this.rollNo = studentRollNo;
        System.out.println("📊 Loading prediction analytics for: " + rollNo);
        loadPredictionData();
    }

    private void loadPredictionData() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            DatabaseService dbService = new DatabaseService(dbManager);
            CGPAPredictionService predictionService = new CGPAPredictionService(dbManager);

            // Get student info
            Student student = dbService.getStudent(rollNo);
            if (student == null) {
                System.err.println("❌ Student not found");
                return;
            }

            String program = student.getProgram() != null ? student.getProgram() : "Unknown";
            int totalSemesters = student.getTotalSemesters() > 0 ? student.getTotalSemesters() : 8;

            lblProgram.setText("Program: " + program);

            // Get detailed prediction
            PredictionDetails prediction = predictionService.getDetailedPrediction(rollNo, totalSemesters, program);

            // Populate main prediction card
            lblPredictedCGPA.setText(prediction.getFormattedPrediction());
            lblConfidence.setText(String.format("%.0f%%", prediction.getConfidence() * 100));
            lblTrend.setText(prediction.getTrend());
            lblProgress.setText(prediction.getProgressText());
            progressBar.setProgress((double) prediction.getCurrentSemester() / prediction.getTotalSemesters());

            // Populate breakdown
            lblPolyPrediction.setText(String.format("%.2f", prediction.getPolynomialPrediction()));
            lblWeightedPrediction.setText(String.format("%.2f", prediction.getWeightedAvgPrediction()));

            // Configure chart axes
            xAxis.setLowerBound(1);
            xAxis.setUpperBound(totalSemesters);
            xAxis.setTickUnit(1);

            // Populate chart
            populateChart(prediction);

            // Populate semester cards
            populateSemesterCards(prediction);

            System.out.println("✅ Prediction analytics loaded");

        } catch (SQLException e) {
            System.err.println("❌ Error loading prediction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateChart(PredictionDetails prediction) {
        gpaChart.getData().clear();
        gpaChart.setLegendVisible(true);

        List<Double> gpas = prediction.getSemesterGPAs();
        List<Double> cgpas = prediction.getSemesterCGPAs();
        List<Double> projected = prediction.getProjectedCGPAs();
        int currentSem = prediction.getCurrentSemester();

        // Series 1: Actual Semester GPA
        XYChart.Series<Number, Number> gpaSeries = new XYChart.Series<>();
        gpaSeries.setName("Semester GPA");
        for (int i = 0; i < gpas.size(); i++) {
            gpaSeries.getData().add(new XYChart.Data<>(i + 1, gpas.get(i)));
        }
        gpaChart.getData().add(gpaSeries);

        // Series 2: Actual CGPA
        XYChart.Series<Number, Number> cgpaSeries = new XYChart.Series<>();
        cgpaSeries.setName("Cumulative GPA");
        for (int i = 0; i < cgpas.size(); i++) {
            cgpaSeries.getData().add(new XYChart.Data<>(i + 1, cgpas.get(i)));
        }
        gpaChart.getData().add(cgpaSeries);

        // Series 3: Projected CGPA (dashed line style)
        if (projected.size() > currentSem) {
            XYChart.Series<Number, Number> projectedSeries = new XYChart.Series<>();
            projectedSeries.setName("Projected CGPA");

            // Start from current semester
            projectedSeries.getData().add(new XYChart.Data<>(currentSem, cgpas.get(currentSem - 1)));

            for (int i = currentSem; i < projected.size(); i++) {
                projectedSeries.getData().add(new XYChart.Data<>(i + 1, projected.get(i)));
            }
            gpaChart.getData().add(projectedSeries);
        }

        // Apply styling after data is loaded
        javafx.application.Platform.runLater(() -> {
            // Style the chart
            gpaChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");

            // Style series
            if (gpaChart.getData().size() >= 1) {
                gpaChart.getData().get(0).getNode().setStyle("-fx-stroke: #4facfe; -fx-stroke-width: 3px;");
            }
            if (gpaChart.getData().size() >= 2) {
                gpaChart.getData().get(1).getNode().setStyle("-fx-stroke: #667eea; -fx-stroke-width: 3px;");
            }
            if (gpaChart.getData().size() >= 3) {
                gpaChart.getData().get(2).getNode()
                        .setStyle("-fx-stroke: #fa709a; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;");
            }
        });
    }

    private void populateSemesterCards(PredictionDetails prediction) {
        semesterCardsPane.getChildren().clear();

        List<Double> gpas = prediction.getSemesterGPAs();
        List<Double> cgpas = prediction.getSemesterCGPAs();

        for (int i = 0; i < gpas.size(); i++) {
            VBox card = createSemesterCard(i + 1, gpas.get(i), cgpas.get(i));
            semesterCardsPane.getChildren().add(card);
        }

        // Add projected semesters with a different style
        List<Double> projected = prediction.getProjectedCGPAs();
        for (int i = gpas.size(); i < projected.size(); i++) {
            VBox card = createProjectedCard(i + 1, projected.get(i));
            semesterCardsPane.getChildren().add(card);
        }
    }

    private VBox createSemesterCard(int semester, double gpa, double cgpa) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        Label semLabel = new Label("Semester " + semester);
        semLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        Label gpaLabel = new Label(String.format("GPA: %.2f", gpa));
        gpaLabel.setStyle("-fx-text-fill: #4facfe; -fx-font-weight: bold;");

        Label cgpaLabel = new Label(String.format("CGPA: %.2f", cgpa));
        cgpaLabel.setStyle("-fx-text-fill: #667eea;");

        card.getChildren().addAll(semLabel, gpaLabel, cgpaLabel);
        return card;
    }

    private VBox createProjectedCard(int semester, double projectedCgpa) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(250,112,154,0.2), rgba(254,225,64,0.2)); "
                        +
                        "-fx-background-radius: 10; -fx-border-color: #fa709a; -fx-border-radius: 10; -fx-border-style: dashed;");

        Label semLabel = new Label("Semester " + semester);
        semLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        Label projLabel = new Label("(Projected)");
        projLabel.setStyle("-fx-text-fill: #fa709a; -fx-font-size: 10;");

        Label cgpaLabel = new Label(String.format("CGPA: %.2f", projectedCgpa));
        cgpaLabel.setStyle("-fx-text-fill: #fa709a; -fx-font-weight: bold;");

        card.getChildren().addAll(semLabel, projLabel, cgpaLabel);
        return card;
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            System.out.println("🔄 Navigating back to dashboard...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DashboardView.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.initializeWithStudent(rollNo);

            Stage stage = (Stage) lblProgram.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            stage.setScene(scene);

            System.out.println("✅ Navigated to dashboard");
        } catch (IOException e) {
            System.err.println("❌ Error navigating to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
