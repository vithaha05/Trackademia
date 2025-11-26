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
     * Predict end semester score based on internal marks
     * Uses linear regression on historical data
     */
    public double predictEndSemScore(String rollNo, String courseCode,
                                     double currentInternalMarks) throws SQLException {
        System.out.println("🤖 Predicting end-sem score for " + courseCode);

        // Get historical data for this course
        List<TrainingData> historicalData = getHistoricalData(courseCode);

        if (historicalData.isEmpty()) {
            // If no historical data, use simple proportion
            // Assuming internal is 40% and endsem is 60% of total
            System.out.println("⚠️ No historical data, using proportional prediction");
            return (currentInternalMarks / 50.0) * 100.0;
        }

        // Simple linear regression: endsem = a + b * internal
        double[] coefficients = calculateLinearRegression(historicalData);
        double intercept = coefficients[0];
        double slope = coefficients[1];

        // Predict end semester score
        double predictedScore = intercept + (slope * currentInternalMarks);

        // Clamp between 0 and 100
        predictedScore = Math.max(0, Math.min(100, predictedScore));

        System.out.println("✅ Predicted score: " + String.format("%.2f", predictedScore));
        return predictedScore;
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

        // Predict end semester score
        double predictedScore = predictEndSemScore(rollNo, courseCode, internalMarks);
        analytics.setPredictedEndsemScore(predictedScore);

        // Calculate percentile
        double percentile = calculateClassPercentile(rollNo, courseCode, semester, internalMarks);
        analytics.setClassPercentile(percentile);

        // Calculate improvement needed to pass (45 marks minimum in endsem)
        double classAverage = calculateClassAverageInternal(courseCode, semester);
        double improvementNeeded = classAverage - internalMarks;
        analytics.setImprovementNeeded(Math.max(0, improvementNeeded));

        // Generate recommendation
        String recommendation = generateRecommendation(internalMarks, predictedScore,
                percentile, classAverage);
        analytics.setRecommendation(recommendation);

        // Save to database
        saveAnalytics(analytics);

        System.out.println("✅ Analytics generated successfully");
        return analytics;
    }

    /**
     * Generate personalized recommendation
     */
    private String generateRecommendation(double internalMarks, double predictedScore,
                                          double percentile, double classAverage) {
        StringBuilder recommendation = new StringBuilder();

        // Performance tier based on percentile
        if (percentile >= 75) {
            recommendation.append("🌟 Excellent performance! You're in the top 25% of the class. ");
        } else if (percentile >= 50) {
            recommendation.append("👍 Good performance! You're above average. ");
        } else if (percentile >= 25) {
            recommendation.append("📚 Average performance. There's room for improvement. ");
        } else {
            recommendation.append("⚠️ Below average performance. Significant improvement needed. ");
        }

        // Prediction-based advice
        if (predictedScore < 45) {
            recommendation.append("🚨 Warning: You need to score at least 45 in end semester to pass. ");
            recommendation.append("Focus on fundamentals and practice previous year questions extensively. ");
        } else if (predictedScore < 60) {
            recommendation.append("You're predicted to pass, but aim higher! ");
            recommendation.append("Review weak topics and solve more practice problems. ");
        } else if (predictedScore < 80) {
            recommendation.append("You're on track for a good score. ");
            recommendation.append("Keep up the consistency and aim for excellence (80+). ");
        } else {
            recommendation.append("🎯 Excellent trajectory! ");
            recommendation.append("Maintain your preparation strategy and stay focused. ");
        }

        // Comparison with class average
        if (classAverage > 0) {
            if (internalMarks < classAverage) {
                recommendation.append(String.format(
                        "You're %.2f marks below class average (%.2f). Work on catching up! ",
                        classAverage - internalMarks, classAverage));
            } else {
                recommendation.append(String.format(
                        "You're %.2f marks above class average (%.2f). Great job! ",
                        internalMarks - classAverage, classAverage));
            }
        }

        return recommendation.toString();
    }

    /**
     * Get historical training data for a course
     */
    private List<TrainingData> getHistoricalData(String courseCode) throws SQLException {
        String sql = """
            SELECT internal_marks, endsem_marks
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
                td.endsemMarks = rs.getDouble("endsem_marks");
                data.add(td);
            }
            rs.close();
        }

        return data;
    }

    /**
     * Calculate linear regression coefficients
     * Returns [intercept, slope]
     * Formula: y = intercept + slope * x
     */
    private double[] calculateLinearRegression(List<TrainingData> data) {
        int n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (TrainingData td : data) {
            sumX += td.internalMarks;
            sumY += td.endsemMarks;
            sumXY += td.internalMarks * td.endsemMarks;
            sumX2 += td.internalMarks * td.internalMarks;
        }

        // Calculate slope (b) and intercept (a)
        // slope = (n*ΣXY - ΣX*ΣY) / (n*ΣX² - (ΣX)²)
        // intercept = (ΣY - slope*ΣX) / n

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        return new double[]{intercept, slope};
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
        double endsemMarks;
    }
}