package com.psgtech.studentportal.models;

/**
 * Internal marks (CA marks) for a course
 */
public class InternalMarks {
    private int id;
    private String rollNo;
    private int semester;
    private String courseCode;
    private String courseName;
    private Double totalInternalMarks;  // Nullable
    private double maxMarks;

    public InternalMarks() {}

    public InternalMarks(String rollNo, int semester, String courseCode,
                         String courseName, Double totalInternalMarks, double maxMarks) {
        this.rollNo = rollNo;
        this.semester = semester;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.totalInternalMarks = totalInternalMarks;
        this.maxMarks = maxMarks;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getRollNo() {
        return rollNo;
    }

    public int getSemester() {
        return semester;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public Double getTotalInternalMarks() {
        return totalInternalMarks;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public void setTotalInternalMarks(Double totalInternalMarks) {
        this.totalInternalMarks = totalInternalMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }

    @Override
    public String toString() {
        return "InternalMarks{" +
                "id=" + id +
                ", rollNo='" + rollNo + '\'' +
                ", semester=" + semester +
                ", courseCode='" + courseCode + '\'' +
                ", courseName='" + courseName + '\'' +
                ", totalInternalMarks=" + totalInternalMarks +
                ", maxMarks=" + maxMarks +
                '}';
    }
}
