package com.campus.tracker.service;

import com.campus.tracker.dao.*;
import com.campus.tracker.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {

    private GradeDAO gradeDAO;
    private SubjectDAO subjectDAO;
    private AttendanceDAO attendanceDAO;

    public AnalyticsService() {
        this.gradeDAO = new GradeDAO();
        this.subjectDAO = new SubjectDAO();
        this.attendanceDAO = new AttendanceDAO();
    }

    /**
     * Calculate CGPA for a student
     */
    public double calculateCGPA(int studentId) {
        List<Subject> subjects = subjectDAO.getSubjectsByStudent(studentId);
        if (subjects.isEmpty()) return 0.0;

        double totalGradePoints = 0.0;
        int totalCredits = 0;

        for (Subject subject : subjects) {
            Grade grade = gradeDAO.getBySubjectId(subject.getId());
            if (grade != null) {
                totalGradePoints += grade.getGradePoint() * subject.getCredits();
                totalCredits += subject.getCredits();
            }
        }

        return totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
    }

    /**
     * Calculate semester-wise CGPA
     */
    public Map<Integer, Double> getSemesterWiseCGPA(int studentId) {
        Map<Integer, Double> semesterCGPA = new TreeMap<>();
        List<Subject> subjects = subjectDAO.getSubjectsByStudent(studentId);

        // Group subjects by semester
        Map<Integer, List<Subject>> subjectsBySemester = subjects.stream()
                .collect(Collectors.groupingBy(Subject::getSemester));

        for (Map.Entry<Integer, List<Subject>> entry : subjectsBySemester.entrySet()) {
            int semester = entry.getKey();
            List<Subject> semesterSubjects = entry.getValue();

            double totalGradePoints = 0.0;
            int totalCredits = 0;

            for (Subject subject : semesterSubjects) {
                Grade grade = gradeDAO.getBySubjectId(subject.getId());
                if (grade != null) {
                    totalGradePoints += grade.getGradePoint() * subject.getCredits();
                    totalCredits += subject.getCredits();
                }
            }

            double semesterGPA = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
            semesterCGPA.put(semester, semesterGPA);
        }

        return semesterCGPA;
    }

    /**
     * Calculate overall attendance percentage
     */
    public double calculateOverallAttendance(int studentId) {
        List<Attendance> attendanceList = attendanceDAO.getAttendanceByStudent(studentId);
        if (attendanceList.isEmpty()) return 0.0;

        int totalAttended = 0;
        int totalClasses = 0;

        for (Attendance attendance : attendanceList) {
            totalAttended += attendance.getClassesAttended();
            totalClasses += attendance.getTotalClasses();
        }

        return totalClasses > 0 ? (totalAttended * 100.0 / totalClasses) : 0.0;
    }

    /**
     * Predict next semester CGPA based on current trend
     */
    public double predictNextSemesterCGPA(int studentId) {
        Map<Integer, Double> semesterCGPAs = getSemesterWiseCGPA(studentId);
        if (semesterCGPAs.size() < 2) {
            return calculateCGPA(studentId); // Not enough data, return current
        }

        List<Double> cgpaList = new ArrayList<>(semesterCGPAs.values());

        // Simple linear trend prediction
        double recentCGPA = cgpaList.get(cgpaList.size() - 1);
        double previousCGPA = cgpaList.get(cgpaList.size() - 2);
        double trend = recentCGPA - previousCGPA;

        // Factor in attendance
        double attendancePercentage = calculateOverallAttendance(studentId);
        double attendanceFactor = (attendancePercentage / 100.0) * 0.2; // 20% weight

        double predicted = recentCGPA + (trend * 0.8) + attendanceFactor;

        // Cap between 0 and 10
        return Math.max(0.0, Math.min(10.0, predicted));
    }

    /**
     * Predict final CGPA at graduation
     */
    public double predictFinalCGPA(int studentId, int totalSemesters) {
        Map<Integer, Double> semesterCGPAs = getSemesterWiseCGPA(studentId);
        int completedSemesters = semesterCGPAs.size();

        if (completedSemesters == 0) return 0.0;
        if (completedSemesters >= totalSemesters) return calculateCGPA(studentId);

        double currentCGPA = calculateCGPA(studentId);
        double predictedNextSem = predictNextSemesterCGPA(studentId);

        // Weight: 70% current, 30% predicted trend
        double avgPrediction = (currentCGPA * 0.7) + (predictedNextSem * 0.3);

        return Math.max(0.0, Math.min(10.0, avgPrediction));
    }

    /**
     * Calculate required GPA to achieve target CGPA
     */
    public double calculateRequiredGPA(int studentId, double targetCGPA, int totalSemesters) {
        Map<Integer, Double> semesterCGPAs = getSemesterWiseCGPA(studentId);
        int completedSemesters = semesterCGPAs.size();

        if (completedSemesters >= totalSemesters) {
            return -1; // Already completed
        }

        double currentCGPA = calculateCGPA(studentId);
        int remainingSemesters = totalSemesters - completedSemesters;

        // Calculate required GPA
        double requiredGPA = (targetCGPA * totalSemesters - currentCGPA * completedSemesters) / remainingSemesters;

        return requiredGPA;
    }

    /**
     * Calculate predicted rank/percentile (simplified model)
     */
    public Map<String, Object> predictRank(int studentId, double avgClassCGPA, double stdDeviation) {
        Map<String, Object> result = new HashMap<>();

        double studentCGPA = calculateCGPA(studentId);

        // Calculate z-score
        double zScore = (studentCGPA - avgClassCGPA) / stdDeviation;

        // Convert to percentile (approximation)
        double percentile = normalCDF(zScore) * 100;

        // Estimate rank (assuming 100 students for demo)
        int estimatedRank = (int) Math.ceil((100 - percentile) / 100.0 * 100);

        result.put("cgpa", studentCGPA);
        result.put("percentile", Math.round(percentile * 100.0) / 100.0);
        result.put("estimatedRank", estimatedRank);
        result.put("totalStudents", 100); // Demo value

        return result;
    }

    /**
     * Check if attendance is at risk for a subject
     */
    public boolean isAttendanceAtRisk(int subjectId, double minRequired) {
        Attendance attendance = attendanceDAO.getBySubjectId(subjectId);
        if (attendance == null) return false;

        return attendance.getPercentage() < minRequired;
    }

    /**
     * Calculate classes needed to reach target attendance
     */
    public int classesNeededForTarget(int subjectId, double targetPercentage) {
        Attendance attendance = attendanceDAO.getBySubjectId(subjectId);
        if (attendance == null) return 0;

        int attended = attendance.getClassesAttended();
        int total = attendance.getTotalClasses();

        // Formula: (attended + x) / (total + x) = target/100
        // Solving for x: x = (target*total - 100*attended) / (100 - target)

        double numerator = (targetPercentage * total) - (100 * attended);
        double denominator = 100 - targetPercentage;

        if (denominator <= 0) return 0;

        int classesNeeded = (int) Math.ceil(numerator / denominator);
        return Math.max(0, classesNeeded);
    }

    /**
     * Get subject-wise performance data
     */
    public List<Map<String, Object>> getSubjectPerformance(int studentId) {
        List<Map<String, Object>> performanceData = new ArrayList<>();
        List<Subject> subjects = subjectDAO.getSubjectsByStudent(studentId);

        for (Subject subject : subjects) {
            Map<String, Object> data = new HashMap<>();
            data.put("subject", subject);

            Grade grade = gradeDAO.getBySubjectId(subject.getId());
            data.put("grade", grade);

            Attendance attendance = attendanceDAO.getBySubjectId(subject.getId());
            data.put("attendance", attendance);

            performanceData.add(data);
        }

        return performanceData;
    }

    // Helper: Normal CDF approximation
    private double normalCDF(double z) {
        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }

    // Helper: Error function approximation
    private double erf(double x) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x));
        double tau = t * Math.exp(-x * x - 1.26551223 +
                t * (1.00002368 +
                        t * (0.37409196 +
                                t * (0.09678418 +
                                        t * (-0.18628806 +
                                                t * (0.27886807 +
                                                        t * (-1.13520398 +
                                                                t * (1.48851587 +
                                                                        t * (-0.82215223 +
                                                                                t * 0.17087277)))))))));

        return x >= 0 ? 1 - tau : tau - 1;
    }
}