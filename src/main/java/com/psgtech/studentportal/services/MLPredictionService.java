package com.psgtech.studentportal.services;

import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.database.DatabaseManager;
import java.sql.*;
import java.util.*;

/**
 * ML Prediction Service
 * Analyzes student performance and predicts end semester scores
 * Uses historical data to provide insights and recommendations
 */
public class MLPredictionService {

    private DatabaseManager dbManager;

    public MLPredictionService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Predict end semester score based on internal marks AND attendance
     * Uses multi-variate linear regression on historical data
     */
    public double predictEndSemScore(String rollNo, String courseCode,
            double currentInternalMarks, double attendancePercentage) throws SQLException {
        System.out
                .println("🤖 Predicting end-sem score for " + courseCode + " with attendance: " + attendancePercentage);

        // Get historical data for this course
        List<TrainingData> historicalData = getHistoricalData(courseCode);

        // Clean data - remove records with missing attendance if we are using it
        // historicalData.removeIf(d -> d.attendance <= 0);

        if (historicalData.size() < 5) {
            // Need sufficient data for regression
            System.out.println("⚠️ Insufficient historical data (" + historicalData.size() + "), using fallback model");
            return calculateFallbackPrediction(currentInternalMarks, attendancePercentage);
        }

        // Multiple Linear Regression: EndSem = b0 + b1*Internal + b2*Attendance
        // Using simple matrix operations or approximation

        // For simplicity and robustness without external libraries, we'll use a
        // weighted approach
        // derived from two separate simple regressions combined, or a simplified MLR
        // implementation

        double[] coefficients = calculateMultiLinearRegression(historicalData);
        double intercept = coefficients[0];
        double slopeInternal = coefficients[1];
        double slopeAttendance = coefficients[2];

        // Predict
        double predictedScore = intercept + (slopeInternal * currentInternalMarks)
                + (slopeAttendance * attendancePercentage);

        // Clamp between 0 and 100
        predictedScore = Math.max(0, Math.min(100, predictedScore));

        System.out.println("✅ Predicted score: " + String.format("%.2f", predictedScore));
        return predictedScore;
    }

    /**
     * Fallback prediction when training data is scarce
     */
    private double calculateFallbackPrediction(double internal, double attendance) {
        // Base: Internal marks projected to 100
        double basePrediction = (internal / 50.0) * 100.0;

        // Adjustment based on attendance
        // If attendance is high (>90), typically slight boost (+2-5%)
        // If attendance is low (<75), penalty (-5-10%)
        if (attendance >= 90) {
            basePrediction += 3.0;
        } else if (attendance < 75) {
            double penalty = (75 - attendance) * 0.5; // 0.5 mark penalty per % shortage
            basePrediction -= penalty;
        }

        return Math.max(0, Math.min(100, basePrediction));
    }

    /**
     * Calculate class percentile for a student
     */
    public double calculateClassPercentile(String rollNo, String courseCode,
            int semester, double internalMarks)
            throws SQLException {
        System.out.println("📊 Calculating percentile for " + courseCode);

        String sql = """
                    SELECT COUNT(*) as total_students,
                           SUM(CASE WHEN total_internal_marks < ? THEN 1 ELSE 0 END) as below_count
                    FROM internal_marks
                    WHERE course_code = ? AND semester = ? AND total_internal_marks IS NOT NULL
                """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, internalMarks);
            stmt.setString(2, courseCode);
            stmt.setInt(3, semester);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int totalStudents = rs.getInt("total_students");
                int belowCount = rs.getInt("below_count");

                if (totalStudents > 1) { // Need at least 2 students for comparison
                    double percentile = (belowCount / (double) totalStudents) * 100.0;
                    System.out.println("✅ Percentile: " + String.format("%.1f", percentile) + "%");
                    return percentile;
                }
            }
            rs.close();
        }

        System.out.println("⚠️ Not enough data for percentile calculation");
        return 50.0; // Default to 50th percentile if no data
    }

    /**
     * Calculate class average for internal marks
     */
    public double calculateClassAverageInternal(String courseCode, int semester)
            throws SQLException {
        String sql = """
                    SELECT AVG(total_internal_marks) as avg_internal
                    FROM internal_marks
                    WHERE course_code = ? AND semester = ? AND total_internal_marks IS NOT NULL
                """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, courseCode);
            stmt.setInt(2, semester);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double avg = rs.getDouble("avg_internal");
                if (!rs.wasNull()) {
                    return avg;
                }
            }
            rs.close();
        }

        return 0.0;
    }

    /**
     * Generate performance analytics for a student
     */
    public PerformanceAnalytics generateAnalytics(String rollNo, String courseCode,
            int semester, double internalMarks)
            throws SQLException {
        System.out.println("🔬 Generating analytics for " + courseCode);

        PerformanceAnalytics analytics = new PerformanceAnalytics();
        analytics.setRollNo(rollNo);
        analytics.setSemester(semester);
        analytics.setCourseCode(courseCode);

        // Get attendance data for this student/course
        double attendance = getAttendance(rollNo, courseCode);

        // Predict end semester score with attendance
        double predictedScore = predictEndSemScore(rollNo, courseCode, internalMarks, attendance);
        analytics.setPredictedEndsemScore(predictedScore);

        // Calculate percentile
        double percentile = calculateClassPercentile(rollNo, courseCode, semester, internalMarks);
        analytics.setClassPercentile(percentile);

        // Calculate improvement needed to pass (45 marks minimum in endsem)
        double classAverage = calculateClassAverageInternal(courseCode, semester);
        double improvementNeeded = classAverage - internalMarks;
        analytics.setImprovementNeeded(Math.max(0, improvementNeeded));

        // Generate recommendation
        String recommendation = generateRecommendation(internalMarks, attendance, predictedScore,
                percentile, classAverage);
        analytics.setRecommendation(recommendation);

        // Save to database
        saveAnalytics(analytics);

        System.out.println("✅ Analytics generated successfully");
        return analytics;
    }

    /**
     * Generate risk categorization
     * 
     * @return "High", "Medium", "Low"
     */
    public String calculateRiskLevel(double internalMarks, double attendance) {
        if (attendance < 75 || internalMarks < 25) { // < 50% internal marks
            return "High";
        } else if (attendance < 80 || internalMarks < 35) { // < 70% internal marks
            return "Medium";
        } else {
            return "Low";
        }
    }

    /**
     * Generate personalized recommendation
     */
    private String generateRecommendation(double internalMarks, double attendance, double predictedScore,
            double percentile, double classAverage) {
        StringBuilder recommendation = new StringBuilder();

        // Risk Assessment
        String riskLevel = calculateRiskLevel(internalMarks, attendance);
        if (riskLevel.equals("High")) {
            recommendation.append("🔴 HIGH RISK: ");
            if (attendance < 75)
                recommendation.append("Attendance is critically low (" + attendance + "%). ");
            if (internalMarks < 25)
                recommendation.append("Internal marks are below 50%. ");
            recommendation.append("Immediate action required to avoid failure. ");
        } else if (riskLevel.equals("Medium")) {
            recommendation.append("🟡 MEDIUM RISK: ");
            if (attendance < 80)
                recommendation.append("Attendance is borderline (" + attendance + "%). ");
            if (internalMarks < 35)
                recommendation.append("Internal marks need improvement. ");
        } else {
            recommendation.append("🟢 LOW RISK: On track. ");
        }

        // Performance tier based on percentile
        if (percentile >= 75) {
            recommendation.append("🌟 Excellent performance! Top 25%. ");
        } else if (percentile >= 50) {
            recommendation.append("👍 Good performance! Above average. ");
        } else if (percentile >= 25) {
            recommendation.append("📚 Average performance. Room for improvement. ");
        } else {
            recommendation.append("⚠️ Below average. ");
        }

        // Prediction-based advice
        if (predictedScore < 45) {
            recommendation.append("🚨 Forecast: Danger zone (<45). ");
            recommendation.append("Must score high in end-sem to pass. ");
        } else if (predictedScore < 60) {
            recommendation.append("Forecast: Passable (50-60). Aim higher! ");
        } else if (predictedScore < 80) {
            recommendation.append("Forecast: Good (60-80). Keep consistent. ");
        } else {
            recommendation.append("Forecast: Excellent (80+). ");
        }

        return recommendation.toString();
    }

    /**
     * Get historical training data for a course
     */
    private List<TrainingData> getHistoricalData(String courseCode) throws SQLException {
        String sql = """
                    SELECT internal_marks, attendance_percentage, endsem_marks
                    FROM ml_training_data
                    WHERE course_code = ?
                    AND internal_marks IS NOT NULL
                    AND endsem_marks IS NOT NULL
                """;

        List<TrainingData> data = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, courseCode);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                TrainingData td = new TrainingData();
                td.internalMarks = rs.getDouble("internal_marks");
                td.attendance = rs.getDouble("attendance_percentage");
                if (rs.wasNull())
                    td.attendance = 75.0; // Default if missing in training data
                td.endsemMarks = rs.getDouble("endsem_marks");
                data.add(td);
            }
            rs.close();
        }

        return data;
    }

    /**
     * Calculate multiple linear regression coefficients
     * Returns [intercept, slopeInternal, slopeAttendance]
     * Formula: y = b0 + b1*x1 + b2*x2
     */
    private double[] calculateMultiLinearRegression(List<TrainingData> data) {
        int n = data.size();
        double sumX1 = 0, sumX2 = 0, sumY = 0;
        double sumX12 = 0, sumX22 = 0, sumX1X2 = 0;
        double sumX1Y = 0, sumX2Y = 0;

        for (TrainingData td : data) {
            sumX1 += td.internalMarks;
            sumX2 += td.attendance;
            sumY += td.endsemMarks;

            sumX12 += td.internalMarks * td.internalMarks;
            sumX22 += td.attendance * td.attendance;
            sumX1X2 += td.internalMarks * td.attendance;

            sumX1Y += td.internalMarks * td.endsemMarks;
            sumX2Y += td.attendance * td.endsemMarks;
        }

        // Solving Normal Equation system for 2 variables
        // This effectively solves (X^T X)^-1 X^T Y

        // Means
        double x1Mean = sumX1 / n;
        double x2Mean = sumX2 / n;
        double yMean = sumY / n;

        // Sums of squares and crossproducts (centered)
        double S11 = sumX12 - n * x1Mean * x1Mean;
        double S22 = sumX22 - n * x2Mean * x2Mean;
        double S12 = sumX1X2 - n * x1Mean * x2Mean;
        double SY1 = sumX1Y - n * x1Mean * yMean;
        double SY2 = sumX2Y - n * x2Mean * yMean;

        // Slopes
        double div = S11 * S22 - S12 * S12;
        if (Math.abs(div) < 1e-9) {
            // Collinear or empty, fallback to simple regression interaction
            return new double[] { 0, 1.2, 0.1 }; // fallback
        }

        double b1 = (SY1 * S22 - SY2 * S12) / div;
        double b2 = (SY2 * S11 - SY1 * S12) / div;

        // Intercept
        double b0 = yMean - b1 * x1Mean - b2 * x2Mean;

        return new double[] { b0, b1, b2 };
    }

    /**
     * Get attendance for student
     */
    private double getAttendance(String rollNo, String courseCode) throws SQLException {
        String sql = "SELECT attendance_percentage FROM internal_marks WHERE roll_no = ? AND course_code = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            stmt.setString(2, courseCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double att = rs.getDouble("attendance_percentage");
                if (!rs.wasNull())
                    return att;
            }
        }
        return 75.0; // Default assumption if missing
    }

    /**
     * Save analytics to database
     */
    private void saveAnalytics(PerformanceAnalytics analytics) throws SQLException {
        String sql = """
                    INSERT INTO performance_analytics
                    (roll_no, semester, course_code, predicted_endsem_score,
                     improvement_needed, class_percentile, recommendation)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        predicted_endsem_score = VALUES(predicted_endsem_score),
                        improvement_needed = VALUES(improvement_needed),
                        class_percentile = VALUES(class_percentile),
                        recommendation = VALUES(recommendation),
                        created_at = CURRENT_TIMESTAMP
                """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, analytics.getRollNo());
            stmt.setInt(2, analytics.getSemester());
            stmt.setString(3, analytics.getCourseCode());
            stmt.setDouble(4, analytics.getPredictedEndsemScore());
            stmt.setDouble(5, analytics.getImprovementNeeded());
            stmt.setDouble(6, analytics.getClassPercentile());
            stmt.setString(7, analytics.getRecommendation());
            stmt.executeUpdate();
        }
    }

    /**
     * Get all analytics for a student
     */
    public List<PerformanceAnalytics> getAnalytics(String rollNo, int semester) throws SQLException {
        String sql = """
                    SELECT * FROM performance_analytics
                    WHERE roll_no = ? AND semester = ?
                    ORDER BY course_code
                """;

        List<PerformanceAnalytics> analyticsList = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            stmt.setInt(2, semester);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                PerformanceAnalytics analytics = new PerformanceAnalytics();
                analytics.setId(rs.getInt("id"));
                analytics.setRollNo(rs.getString("roll_no"));
                analytics.setSemester(rs.getInt("semester"));
                analytics.setCourseCode(rs.getString("course_code"));
                analytics.setPredictedEndsemScore(rs.getDouble("predicted_endsem_score"));
                analytics.setImprovementNeeded(rs.getDouble("improvement_needed"));
                analytics.setClassPercentile(rs.getDouble("class_percentile"));
                analytics.setRecommendation(rs.getString("recommendation"));
                analyticsList.add(analytics);
            }
            rs.close();
        }

        return analyticsList;
    }

    /**
     * Inner class for training data
     */
    private static class TrainingData {
        double internalMarks;
        double attendance;
        double endsemMarks;
    }

    /**
     * Get attendance sensitivity analysis
     * Returns a map of Attendance % -> Predicted Score
     */
    public Map<Integer, Double> getAttendanceSensitivity(String courseCode, double internalMarks) {
        Map<Integer, Double> sensitivity = new TreeMap<>();

        try {
            // Get historical data to calculate coefficients
            List<TrainingData> historical = getHistoricalData(courseCode);
            double[] coeffs;

            if (historical.size() >= 5) {
                coeffs = calculateMultiLinearRegression(historical);
            } else {
                // Fallback coefficients
                coeffs = new double[] { 0, 1.2, 0.1 };
            }

            double intercept = coeffs[0];
            double slopeInternal = coeffs[1];
            double slopeAttendance = coeffs[2];

            // Generate predictions for attendance from 50% to 100% in steps of 5%
            for (int att = 50; att <= 100; att += 5) {
                double predicted = intercept + (slopeInternal * internalMarks) + (slopeAttendance * att);

                // If using fallback, use the specialized fallback logic instead
                if (historical.size() < 5) {
                    predicted = calculateFallbackPrediction(internalMarks, att);
                }

                predicted = Math.max(0, Math.min(100, predicted));
                sensitivity.put(att, predicted);
            }

        } catch (SQLException e) {
            System.err.println("⚠️ Error calculating sensitivity: " + e.getMessage());
        }

        return sensitivity;
    }
}