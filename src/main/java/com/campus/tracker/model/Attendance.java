package com.campus.tracker.model;

public class Attendance {
    private int id;
    private int subjectId;
    private int classesAttended;
    private int totalClasses;
    private double percentage;

    // Constructors
    public Attendance() {}

    public Attendance(int subjectId, int classesAttended, int totalClasses) {
        this.subjectId = subjectId;
        this.classesAttended = classesAttended;
        this.totalClasses = totalClasses;
        this.percentage = totalClasses > 0 ? (classesAttended * 100.0 / totalClasses) : 0.0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public int getClassesAttended() { return classesAttended; }
    public void setClassesAttended(int classesAttended) {
        this.classesAttended = classesAttended;
        updatePercentage();
    }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
        updatePercentage();
    }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    private void updatePercentage() {
        this.percentage = totalClasses > 0 ? (classesAttended * 100.0 / totalClasses) : 0.0;
    }
}