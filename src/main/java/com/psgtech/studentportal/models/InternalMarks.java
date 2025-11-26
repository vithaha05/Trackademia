package com.psgtech.studentportal.models;

import java.time.LocalDateTime;

/**
 * Internal Marks Model
 */
class InternalMarks {
    private int id;
    private String rollNo;
    private int semester;
    private String courseCode;
    private String courseName;
    private Double ca1Marks;
    private Double ca2Marks;
    private Double ca3Marks;
    private Double totalInternalMarks;
    private double maxMarks = 50.0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Double getCa1Marks() { return ca1Marks; }
    public void setCa1Marks(Double ca1Marks) { this.ca1Marks = ca1Marks; }

    public Double getCa2Marks() { return ca2Marks; }
    public void setCa2Marks(Double ca2Marks) { this.ca2Marks = ca2Marks; }

    public Double getCa3Marks() { return ca3Marks; }
    public void setCa3Marks(Double ca3Marks) { this.ca3Marks = ca3Marks; }

    public Double getTotalInternalMarks() { return totalInternalMarks; }
    public void setTotalInternalMarks(Double totalInternalMarks) {
        this.totalInternalMarks = totalInternalMarks;
    }

    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
