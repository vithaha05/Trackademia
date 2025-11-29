package com.psgtech.studentportal.models;

import java.time.LocalDate;

/**
 * Student Model Class
 * Represents a student entity in the system
 */
public class Student {
    private String rollNo;
    private String name;
    private LocalDate dateOfBirth;  // Changed to LocalDate
    private String department;
    private String batch;
    private int currentSemester;  // Changed to int

    // Default Constructor
    public Student() {
    }

    // Parameterized Constructor
    public Student(String rollNo, String name, LocalDate dateOfBirth,
                   String department, String batch, int currentSemester) {
        this.rollNo = rollNo;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.department = department;
        this.batch = batch;
        this.currentSemester = currentSemester;
    }

    // Getters
    public String getRollNo() {
        return rollNo;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDepartment() {
        return department;
    }

    public String getBatch() {
        return batch;
    }

    public int getCurrentSemester() {
        return currentSemester;
    }

    // Setters
    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public void setCurrentSemester(int currentSemester) {
        this.currentSemester = currentSemester;
    }

    @Override
    public String toString() {
        return "Student{" +
                "rollNo='" + rollNo + '\'' +
                ", name='" + name + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", department='" + department + '\'' +
                ", batch='" + batch + '\'' +
                ", currentSemester=" + currentSemester +
                '}';
    }
}