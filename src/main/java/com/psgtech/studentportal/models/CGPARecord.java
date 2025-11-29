package com.psgtech.studentportal.models;

/**
 * CGPA Record for tracking semester-wise GPA and CGPA
 */
public class CGPARecord {
    private int id;
    private String rollNo;
    private int semester;
    private Double gpa;           // Nullable
    private Double cgpa;          // Nullable
    private Integer totalCredits; // Nullable
    private boolean hasBacklogs;

    public CGPARecord() {}

    public CGPARecord(String rollNo, int semester, Double gpa, Double cgpa,
                      Integer totalCredits, boolean hasBacklogs) {
        this.rollNo = rollNo;
        this.semester = semester;
        this.gpa = gpa;
        this.cgpa = cgpa;
        this.totalCredits = totalCredits;
        this.hasBacklogs = hasBacklogs;
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

    public Double getGpa() {
        return gpa;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public Integer getTotalCredits() {
        return totalCredits;
    }

    public boolean isHasBacklogs() {
        return hasBacklogs;
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

    public void setGpa(Double gpa) {
        this.gpa = gpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    public void setTotalCredits(Integer totalCredits) {
        this.totalCredits = totalCredits;
    }

    public void setHasBacklogs(boolean hasBacklogs) {
        this.hasBacklogs = hasBacklogs;
    }

    @Override
    public String toString() {
        return "CGPARecord{" +
                "id=" + id +
                ", rollNo='" + rollNo + '\'' +
                ", semester=" + semester +
                ", gpa=" + gpa +
                ", cgpa=" + cgpa +
                ", totalCredits=" + totalCredits +
                ", hasBacklogs=" + hasBacklogs +
                '}';
    }
}
