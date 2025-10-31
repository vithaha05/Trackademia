package com.campus.tracker.util;

import com.campus.tracker.dao.*;
import com.campus.tracker.model.*;

public class DataSeeder {

    public static void seedSampleData(int studentId) {
        SubjectDAO subjectDAO = new SubjectDAO();
        GradeDAO gradeDAO = new GradeDAO();
        AttendanceDAO attendanceDAO = new AttendanceDAO();

        // Semester 1 subjects
        Subject[] semester1 = {
                new Subject(studentId, "CS101", "Data Structures", 4, 1),
                new Subject(studentId, "MA101", "Calculus I", 4, 1),
                new Subject(studentId, "PH101", "Physics", 3, 1),
                new Subject(studentId, "EN101", "English", 3, 1),
                new Subject(studentId, "CS102", "Programming", 4, 1)
        };

        double[] internal1 = {38, 35, 32, 36, 40};
        double[] external1 = {55, 52, 48, 54, 58};
        double[] total1 = {93, 87, 80, 90, 98};
        double[] gp1 = {9.3, 8.7, 8.0, 9.0, 9.8};
        String[] grades1 = {"O", "A", "A", "A+", "O"};

        int[][] attendance1 = {
                {42, 45}, {38, 45}, {40, 42}, {35, 42}, {43, 45}
        };

        // Add semester 1
        for (int i = 0; i < semester1.length; i++) {
            subjectDAO.addSubject(semester1[i]);

            Grade grade = new Grade(
                    semester1[i].getId(),
                    internal1[i], external1[i], total1[i], gp1[i], grades1[i]
            );
            gradeDAO.addGrade(grade);

            Attendance attendance = new Attendance(
                    semester1[i].getId(),
                    attendance1[i][0], attendance1[i][1]
            );
            attendanceDAO.addAttendance(attendance);
        }

        // Semester 2 subjects
        Subject[] semester2 = {
                new Subject(studentId, "CS201", "Algorithms", 4, 2),
                new Subject(studentId, "MA201", "Linear Algebra", 4, 2),
                new Subject(studentId, "CS202", "Database Systems", 4, 2),
                new Subject(studentId, "CS203", "Operating Systems", 3, 2),
                new Subject(studentId, "EC201", "Digital Electronics", 3, 2)
        };

        double[] internal2 = {39, 37, 38, 35, 36};
        double[] external2 = {56, 54, 57, 50, 52};
        double[] total2 = {95, 91, 95, 85, 88};
        double[] gp2 = {9.5, 9.1, 9.5, 8.5, 8.8};
        String[] grades2 = {"O", "A+", "O", "A", "A"};

        int[][] attendance2 = {
                {40, 42}, {35, 42}, {41, 42}, {38, 40}, {36, 40}
        };

        // Add semester 2
        for (int i = 0; i < semester2.length; i++) {
            subjectDAO.addSubject(semester2[i]);

            Grade grade = new Grade(
                    semester2[i].getId(),
                    internal2[i], external2[i], total2[i], gp2[i], grades2[i]
            );
            gradeDAO.addGrade(grade);

            Attendance attendance = new Attendance(
                    semester2[i].getId(),
                    attendance2[i][0], attendance2[i][1]
            );
            attendanceDAO.addAttendance(attendance);
        }

        // Current semester (Semester 3) - in progress, no grades yet
        Subject[] semester3 = {
                new Subject(studentId, "CS301", "Machine Learning", 4, 3),
                new Subject(studentId, "CS302", "Computer Networks", 4, 3),
                new Subject(studentId, "CS303", "Software Engineering", 3, 3),
                new Subject(studentId, "MA301", "Probability & Statistics", 4, 3)
        };

        int[][] attendance3 = {
                {28, 35}, {32, 35}, {26, 30}, {30, 35}
        };

        // Add semester 3 (grades not yet available)
        for (int i = 0; i < semester3.length; i++) {
            subjectDAO.addSubject(semester3[i]);

            Attendance attendance = new Attendance(
                    semester3[i].getId(),
                    attendance3[i][0], attendance3[i][1]
            );
            attendanceDAO.addAttendance(attendance);
        }

        System.out.println("✅ Sample data seeded successfully!");
    }
}