package com.psgtech.studentportal.models;

import java.time.LocalDateTime;

/**
 * End Semester Marks Model
 */
class EndSemMarks {
    private int id;
    private String rollNo;
    private int semester;
    private String courseCode;
    private String courseName;
    private Double endsemMarks;
    private double maxMarks = 100.0;
    private Double finalMarks;
    private String grade;
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

    public Double getEndsemMarks() { return endsemMarks; }
    public void setEndsemMarks(Double endsemMarks) { this.endsemMarks = endsemMarks; }

    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }

    public Double getFinalMarks() { return finalMarks; }
    public void setFinalMarks(Double finalMarks) { this.finalMarks = finalMarks; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

