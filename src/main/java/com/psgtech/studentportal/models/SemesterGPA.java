package com.psgtech.studentportal.models;

/**
 * Semester-wise GPA and CGPA data
 */
public class SemesterGPA {
    private int semester;
    private String gpa;
    private String cgpa;

    public SemesterGPA() {}

    public SemesterGPA(int semester, String gpa, String cgpa) {
        this.semester = semester;
        this.gpa = gpa;
        this.cgpa = cgpa;
    }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getGpa() { return gpa; }
    public void setGpa(String gpa) { this.gpa = gpa; }

    public String getCgpa() { return cgpa; }
    public void setCgpa(String cgpa) { this.cgpa = cgpa; }

    @Override
    public String toString() {
        return "SemesterGPA{" +
                "semester=" + semester +
                ", gpa='" + gpa + '\'' +
                ", cgpa='" + cgpa + '\'' +
                '}';
    }
}
