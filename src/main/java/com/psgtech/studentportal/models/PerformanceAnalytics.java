package com.psgtech.studentportal.models;

import java.util.List;

/**
 * Performance Analytics Model
 * Contains detailed analytics about student performance and predictions
 */
public class PerformanceAnalytics {
    // Basic identifiers
    private int id;
    private String rollNo;
    private int semester;
    private String courseCode;

    // Current performance metrics
    private double currentCGPA;
    private double predictedCGPA;
    private String performanceCategory; // "Excellent", "Good", "Average", "Below Average", "Poor"
    private int totalBacklogs;
    private double attendancePercentage;

    // Prediction metrics
    private double predictedEndsemScore;
    private double improvementNeeded;
    private double classPercentile;
    private String recommendation;

    // Advanced analytics
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private SemesterTrend semesterTrend;
    private RiskAnalysis riskAnalysis;

    public PerformanceAnalytics() {}

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public double getCurrentCGPA() {
        return currentCGPA;
    }

    public void setCurrentCGPA(double currentCGPA) {
        this.currentCGPA = currentCGPA;
    }

    public double getPredictedCGPA() {
        return predictedCGPA;
    }

    public void setPredictedCGPA(double predictedCGPA) {
        this.predictedCGPA = predictedCGPA;
    }

    public String getPerformanceCategory() {
        return performanceCategory;
    }

    public void setPerformanceCategory(String performanceCategory) {
        this.performanceCategory = performanceCategory;
    }

    public int getTotalBacklogs() {
        return totalBacklogs;
    }

    public void setTotalBacklogs(int totalBacklogs) {
        this.totalBacklogs = totalBacklogs;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public double getPredictedEndsemScore() {
        return predictedEndsemScore;
    }

    public void setPredictedEndsemScore(double predictedEndsemScore) {
        this.predictedEndsemScore = predictedEndsemScore;
    }

    public double getImprovementNeeded() {
        return improvementNeeded;
    }

    public void setImprovementNeeded(double improvementNeeded) {
        this.improvementNeeded = improvementNeeded;
    }

    public double getClassPercentile() {
        return classPercentile;
    }

    public void setClassPercentile(double classPercentile) {
        this.classPercentile = classPercentile;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public SemesterTrend getSemesterTrend() {
        return semesterTrend;
    }

    public void setSemesterTrend(SemesterTrend semesterTrend) {
        this.semesterTrend = semesterTrend;
    }

    public RiskAnalysis getRiskAnalysis() {
        return riskAnalysis;
    }

    public void setRiskAnalysis(RiskAnalysis riskAnalysis) {
        this.riskAnalysis = riskAnalysis;
    }

    @Override
    public String toString() {
        return "PerformanceAnalytics{" +
                "id=" + id +
                ", rollNo='" + rollNo + '\'' +
                ", semester=" + semester +
                ", courseCode='" + courseCode + '\'' +
                ", currentCGPA=" + currentCGPA +
                ", predictedCGPA=" + predictedCGPA +
                ", performanceCategory='" + performanceCategory + '\'' +
                ", totalBacklogs=" + totalBacklogs +
                ", attendancePercentage=" + attendancePercentage +
                ", predictedEndsemScore=" + predictedEndsemScore +
                ", improvementNeeded=" + improvementNeeded +
                ", classPercentile=" + classPercentile +
                ", recommendation='" + recommendation + '\'' +
                ", strengths=" + strengths +
                ", weaknesses=" + weaknesses +
                ", recommendations=" + recommendations +
                ", semesterTrend=" + semesterTrend +
                ", riskAnalysis=" + riskAnalysis +
                '}';
    }

    /**
     * Inner class for Semester Trend Analysis
     */
    public static class SemesterTrend {
        private String trend; // "Improving", "Declining", "Stable"
        private double trendPercentage;
        private List<Double> semesterGPAs;

        public SemesterTrend() {}

        public SemesterTrend(String trend, double trendPercentage, List<Double> semesterGPAs) {
            this.trend = trend;
            this.trendPercentage = trendPercentage;
            this.semesterGPAs = semesterGPAs;
        }

        public String getTrend() {
            return trend;
        }

        public void setTrend(String trend) {
            this.trend = trend;
        }

        public double getTrendPercentage() {
            return trendPercentage;
        }

        public void setTrendPercentage(double trendPercentage) {
            this.trendPercentage = trendPercentage;
        }

        public List<Double> getSemesterGPAs() {
            return semesterGPAs;
        }

        public void setSemesterGPAs(List<Double> semesterGPAs) {
            this.semesterGPAs = semesterGPAs;
        }

        @Override
        public String toString() {
            return "SemesterTrend{" +
                    "trend='" + trend + '\'' +
                    ", trendPercentage=" + trendPercentage +
                    ", semesterGPAs=" + semesterGPAs +
                    '}';
        }
    }

    /**
     * Inner class for Risk Analysis
     */
    public static class RiskAnalysis {
        private String riskLevel; // "Low", "Medium", "High"
        private double riskScore; // 0-100
        private List<String> riskFactors;
        private String interventionRequired; // "None", "Monitor", "Immediate"

        public RiskAnalysis() {}

        public RiskAnalysis(String riskLevel, double riskScore, List<String> riskFactors, String interventionRequired) {
            this.riskLevel = riskLevel;
            this.riskScore = riskScore;
            this.riskFactors = riskFactors;
            this.interventionRequired = interventionRequired;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public double getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(double riskScore) {
            this.riskScore = riskScore;
        }

        public List<String> getRiskFactors() {
            return riskFactors;
        }

        public void setRiskFactors(List<String> riskFactors) {
            this.riskFactors = riskFactors;
        }

        public String getInterventionRequired() {
            return interventionRequired;
        }

        public void setInterventionRequired(String interventionRequired) {
            this.interventionRequired = interventionRequired;
        }

        @Override
        public String toString() {
            return "RiskAnalysis{" +
                    "riskLevel='" + riskLevel + '\'' +
                    ", riskScore=" + riskScore +
                    ", riskFactors=" + riskFactors +
                    ", interventionRequired='" + interventionRequired + '\'' +
                    '}';
        }
    }
}