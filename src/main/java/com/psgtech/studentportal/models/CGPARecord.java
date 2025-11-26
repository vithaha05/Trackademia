package com.psgtech.studentportal.models;

import java.time.LocalDateTime;

/**
 * CGPA Record Model
 */
class CGPARecord {
    private int id;
    private String rollNo;
    private int semester;
    private Double gpa;
    private Double cgpa;
    private Integer totalCredits;
    private boolean hasBacklogs;
    private LocalDateTime createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }

    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }

    public Integer getTotalCredits() { return totalCredits; }
    public void setTotalCredits(Integer totalCredits) { this.totalCredits = totalCredits; }

    public boolean isHasBacklogs() { return hasBacklogs; }
    public void setHasBacklogs(boolean hasBacklogs) { this.hasBacklogs = hasBacklogs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
