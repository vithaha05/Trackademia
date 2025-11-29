package com.psgtech.studentportal.models;

/**
 * Course model representing a course with grades and credits
 */
public class Course {
    private int id;
    private String rollNo;
    private int semester;
    private String courseCode;
    private String courseName;
    private int credits;
    private String grade;
    private double gradePoints;

    public Course() {}

    public Course(String rollNo, int semester, String courseCode, String courseName,
                  int credits, String grade, double gradePoints) {
        this.rollNo = rollNo;
        this.semester = semester;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.grade = grade;
        this.gradePoints = gradePoints;
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

    public int getCredits() {
        return credits;
    }

    public String getGrade() {
        return grade;
    }

    public double getGradePoints() {
        return gradePoints;
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

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void setGradePoints(double gradePoints) {
        this.gradePoints = gradePoints;
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", rollNo='" + rollNo + '\'' +
                ", semester=" + semester +
                ", courseCode='" + courseCode + '\'' +
                ", courseName='" + courseName + '\'' +
                ", credits=" + credits +
                ", grade='" + grade + '\'' +
                ", gradePoints=" + gradePoints +
                '}';
    }
}
