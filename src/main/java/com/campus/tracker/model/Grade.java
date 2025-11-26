package com.campus.tracker.model;

public class Grade {
    private int id;
    private int subjectId;
    private double internalMarks;
    private double externalMarks;
    private double totalMarks;
    private double gradePoint;
    private String grade;

    // Constructors
    public Grade() {}

    public Grade(int subjectId, double internalMarks, double externalMarks,
                 double totalMarks, double gradePoint, String grade) {
        this.subjectId = subjectId;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.totalMarks = totalMarks;
        this.gradePoint = gradePoint;
        this.grade = grade;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public double getInternalMarks() { return internalMarks; }
    public void setInternalMarks(double internalMarks) { this.internalMarks = internalMarks; }

    public double getExternalMarks() { return externalMarks; }
    public void setExternalMarks(double externalMarks) { this.externalMarks = externalMarks; }

    public double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(double totalMarks) { this.totalMarks = totalMarks; }

    public double getGradePoint() { return gradePoint; }
    public void setGradePoint(double gradePoint) { this.gradePoint = gradePoint; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}