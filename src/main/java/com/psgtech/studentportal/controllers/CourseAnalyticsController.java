package com.psgtech.studentportal.controllers;

import com.psgtech.studentportal.models.PerformanceAnalytics;
import com.psgtech.studentportal.services.DatabaseService;
import com.psgtech.studentportal.services.MLPredictionService;
import com.psgtech.studentportal.database.DatabaseManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class CourseAnalyticsController {

    @FXML
    private Label lblCourseName;
    @FXML
    private Label lblCourseCode;
    @FXML
    private Label lblInternalMarks;
    @FXML
    private Label lblAttendance;
    @FXML
    private Label lblPercentile;
    @FXML
    private LineChart<Number, Number> chartSensitivity;
    @FXML
    private Circle riskIndicator;
    @FXML
    private Label lblRiskLevel;
    @FXML
    private Label lblImprovement;
    @FXML
    private Label lblRecommendation;

    private DatabaseService databaseService;
    private MLPredictionService mlService;
    private String rollNo;
    private String courseCode;

    public void initializeWithCourse(String rollNo, String courseCode, String courseName,
            int semester, double internalMarks, double attendance) {
        this.rollNo = rollNo;
        this.courseCode = courseCode;

        DatabaseManager dbManager = DatabaseManager.getInstance();
        this.databaseService = new DatabaseService(dbManager);
        this.mlService = new MLPredictionService(dbManager);

        // Set Basic Info
        lblCourseName.setText(courseName != null ? courseName : "Course Analytics");
        lblCourseCode.setText(courseCode);
        lblInternalMarks.setText(String.format("%.1f / 50", internalMarks));
        lblAttendance.setText(String.format("%.1f%%", attendance));

        // Async load analysis to avoid freezing UI
        new Thread(() -> {
            try {
                // 1. Generate Analytics
                PerformanceAnalytics analytics = mlService.generateAnalytics(rollNo, courseCode, semester,
                        internalMarks);

                // 2. Get Sensitivity Data
                Map<Integer, Double> sensitivity = mlService.getAttendanceSensitivity(courseCode, internalMarks);

                // Update UI on FX Thread
                Platform.runLater(() -> {
                    updateUI(analytics, sensitivity);
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> lblRecommendation.setText("Error loading analytics: " + e.getMessage()));
            }
        }).start();
    }

    private void updateUI(PerformanceAnalytics analytics, Map<Integer, Double> sensitivity) {
        // Update Percentile
        lblPercentile.setText(String.format("Top %.0f%%", 100 - analytics.getClassPercentile()));

        // Update Recommendation
        lblRecommendation.setText(analytics.getRecommendation());

        // Update Risk
        if (analytics.getRecommendation().contains("HIGH RISK")) {
            riskIndicator.setFill(Color.RED);
            lblRiskLevel.setText("High Risk");
        } else if (analytics.getRecommendation().contains("MEDIUM RISK")) {
            riskIndicator.setFill(Color.ORANGE);
            lblRiskLevel.setText("Medium Risk");
        } else {
            riskIndicator.setFill(Color.GREEN);
            lblRiskLevel.setText("Low Risk");
        }

        // Update Improvement
        if (analytics.getImprovementNeeded() > 0) {
            lblImprovement
                    .setText(String.format("Need +%.1f marks to reach class avg.", analytics.getImprovementNeeded()));
        } else {
            lblImprovement.setText("You are above class average!");
        }

        // Populate Chart
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Predicted Score");

        for (Map.Entry<Integer, Double> entry : sensitivity.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        chartSensitivity.getData().clear();
        chartSensitivity.getData().add(series);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DashboardView.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.initializeWithStudent(rollNo);

            Stage stage = (Stage) lblCourseName.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
