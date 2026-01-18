package com.psgtech.studentportal.controllers;

import com.psgtech.studentportal.database.DatabaseManager;
import com.psgtech.studentportal.services.AnalyticsService;
import com.psgtech.studentportal.utils.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class InsightsController {

    @FXML
    private BarChart<String, Number> chartImpact;
    @FXML
    private BarChart<String, Number> chartTrend;
    @FXML
    private ListView<String> listInsights;
    @FXML
    private Button btnBack; // Using fx:id for scene access if needed

    private String rollNo;
    private AnalyticsService analyticsService;
    private ThemeManager themeManager;

    @FXML
    public void initialize() {
        themeManager = ThemeManager.getInstance();
    }

    public void initializeWithStudent(String rollNo) {
        this.rollNo = rollNo;
        try {
            analyticsService = new AnalyticsService(DatabaseManager.getInstance());
            loadAnalytics();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAnalytics() throws SQLException {
        // 1. Category Impact Chart
        Map<String, Double> impact = analyticsService.calculateCategoryImpact(rollNo);
        XYChart.Series<String, Number> seriesImpact = new XYChart.Series<>();
        seriesImpact.setName("GPA Impact");
        seriesImpact.getData().add(new XYChart.Data<>("Theory", impact.getOrDefault("Theory", 0.0)));
        seriesImpact.getData().add(new XYChart.Data<>("Laboratory", impact.getOrDefault("Laboratory", 0.0)));
        chartImpact.getData().clear();
        chartImpact.getData().add(seriesImpact);

        // 2. Semester Trend Chart
        Map<Integer, Double> history = analyticsService.getSemesterHistory(rollNo);
        XYChart.Series<String, Number> seriesTrend = new XYChart.Series<>();
        seriesTrend.setName("Semester GPA");

        for (Map.Entry<Integer, Double> entry : history.entrySet()) {
            seriesTrend.getData().add(new XYChart.Data<>(String.valueOf(entry.getKey()), entry.getValue()));
        }
        chartTrend.getData().clear();
        chartTrend.getData().add(seriesTrend);

        // 3. Automated Insights
        List<String> insights = analyticsService.generateInsights(rollNo);
        ObservableList<String> items = FXCollections.observableArrayList(insights);
        listInsights.setItems(items);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DashboardView.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.initializeWithStudent(rollNo); // Re-init dashboard

            Stage stage = (Stage) chartImpact.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            themeManager.applyTheme(scene); // Apply current theme

            stage.setScene(scene);
            stage.setTitle("Tracademia - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
