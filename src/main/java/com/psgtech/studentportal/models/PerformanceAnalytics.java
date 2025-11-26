package com.psgtech.studentportal.models;

import java.time.LocalDateTime;

/**
 * Performance Analytics Model (ML Predictions)
 */
class PerformanceAnalytics {
    private int id;
    private String rollNo;
    private int semester;
    private String courseCode;
    private Double predictedEndsemScore;
    private Double improvementNeeded;
    private Double classPercentile;
    private String recommendation;
    private LocalDateTime createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public Double getPredictedEndsemScore() { return predictedEndsemScore; }
    public void setPredictedEndsemScore(Double predictedEndsemScore) {
        this.predictedEndsemScore = predictedEndsemScore;
    }

    public Double getImprovementNeeded() { return improvementNeeded; }
    public void setImprovementNeeded(Double improvementNeeded) {
        this.improvementNeeded = improvementNeeded;
    }

    public Double getClassPercentile() { return classPercentile; }
    public void setClassPercentile(Double classPercentile) {
        this.classPercentile = classPercentile;
    }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
