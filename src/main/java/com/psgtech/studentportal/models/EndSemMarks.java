package com.psgtech.studentportal.models;

/**
 * End Semester marks for a course
 */
public class EndSemMarks {
    private int id;
    private String rollNo;
    private int semester;
    private String courseCode;
    private String courseName;
    private Double endsemMarks;  // Nullable
    private double maxMarks;
    private Double finalMarks;   // Nullable
    private String grade;

    public EndSemMarks() {}

    public EndSemMarks(String rollNo, int semester, String courseCode,
                       String courseName, Double endsemMarks, double maxMarks,
                       Double finalMarks, String grade) {
        this.rollNo = rollNo;
        this.semester = semester;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.endsemMarks = endsemMarks;
        this.maxMarks = maxMarks;
        this.finalMarks = finalMarks;
        this.grade = grade;
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

    public Double getEndsemMarks() {
        return endsemMarks;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public Double getFinalMarks() {
        return finalMarks;
    }

    public String getGrade() {
        return grade;
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

    public void setEndsemMarks(Double endsemMarks) {
        this.endsemMarks = endsemMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public void setFinalMarks(Double finalMarks) {
        this.finalMarks = finalMarks;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    @Override
    public String toString() {
        return "EndSemMarks{" +
                "id=" + id +
                ", rollNo='" + rollNo + '\'' +
                ", semester=" + semester +
                ", courseCode='" + courseCode + '\'' +
                ", courseName='" + courseName + '\'' +
                ", endsemMarks=" + endsemMarks +
                ", maxMarks=" + maxMarks +
                ", finalMarks=" + finalMarks +
                ", grade='" + grade + '\'' +
                '}';
    }
}