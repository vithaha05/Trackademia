package com.psgtech.studentportal.services;

import com.psgtech.studentportal.database.DatabaseManager;
import com.psgtech.studentportal.models.Course;
import com.psgtech.studentportal.models.InternalMarks;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {
    private DatabaseService databaseService;

    public AnalyticsService(DatabaseManager dbManager) throws SQLException {
        this.databaseService = new DatabaseService(dbManager);
    }

    public Map<String, Double> calculateCategoryImpact(String rollNo) throws SQLException {
        List<Course> courses = databaseService.getCourses(rollNo);
        Map<String, List<Course>> coursesByCategory = new HashMap<>();
        Map<String, Double> impactMap = new HashMap<>();

        // Group by category (Lab vs Theory)
        for (Course course : courses) {
            String category = extractCategory(course.getCourseName(), course.getCredits());
            coursesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(course);
        }

        // Calculate average GPA for each category
        for (String category : coursesByCategory.keySet()) {
            List<Course> catCourses = coursesByCategory.get(category);
            double totalPoints = 0;
            double totalCredits = 0;

            for (Course c : catCourses) {
                totalPoints += c.getGradePoints() * c.getCredits();
                totalCredits += c.getCredits();
            }

            double avgGPA = (totalCredits > 0) ? (totalPoints / totalCredits) : 0.0;
            impactMap.put(category, avgGPA);
        }

        return impactMap;
    }

    public Map<Integer, Double> getSemesterHistory(String rollNo) throws SQLException {
        List<Course> allCourses = databaseService.getCourses(rollNo);
        Map<Integer, List<Course>> bySem = allCourses.stream().collect(Collectors.groupingBy(Course::getSemester));
        Map<Integer, Double> history = new TreeMap<>(); // TreeMap for sorted keys

        for (Map.Entry<Integer, List<Course>> entry : bySem.entrySet()) {
            history.put(entry.getKey(), calculateSemesterGPA(entry.getValue()));
        }
        return history;
    }

    public double calculateConsistencyScore(String rollNo) throws SQLException {
        Map<Integer, Double> history = getSemesterHistory(rollNo);
        if (history.isEmpty())
            return 0.0;

        double sum = 0;
        for (double gpa : history.values())
            sum += gpa;
        double mean = sum / history.size();

        double sqSum = 0;
        for (double gpa : history.values())
            sqSum += Math.pow(gpa - mean, 2);

        // Standard Deviation
        return Math.sqrt(sqSum / history.size());
    }

    public List<String> generateInsights(String rollNo) throws SQLException {
        List<String> insights = new ArrayList<>();
        Map<String, Double> categoryImpact = calculateCategoryImpact(rollNo);

        // 1. Consistency Analysis
        double stdDev = calculateConsistencyScore(rollNo);
        String consistencyMsg;
        if (stdDev < 0.3)
            consistencyMsg = "🌟 You are an extremely consistent performer!";
        else if (stdDev < 0.7)
            consistencyMsg = "✅ Your performance is stable across semesters.";
        else
            consistencyMsg = "⚠️ Your performance shows high variance (volatility).";
        insights.add(String.format("%s (Consistency Score: %.2f)", consistencyMsg, stdDev));

        // 2. Category Analysis
        double theoryGPA = categoryImpact.getOrDefault("Theory", 0.0);
        double labGPA = categoryImpact.getOrDefault("Laboratory", 0.0);

        if (theoryGPA > 0 && labGPA > 0) {
            double diff = Math.abs(theoryGPA - labGPA);
            if (labGPA < theoryGPA - 0.5) {
                insights.add(
                        String.format("⚠️ Labs are dragging your CGPA down by %.2f points compared to Theory.", diff));
            } else if (labGPA > theoryGPA + 0.5) {
                insights.add(String.format("🚀 You perform significantly better in Labs (+%.2f) than Theory.", diff));
            } else {
                insights.add("⚖️ balanced performance between Labs and Theory.");
            }
        }

        // 3. Trend Analysis
        Map<Integer, Double> history = getSemesterHistory(rollNo);
        if (history.size() >= 2) {
            List<Integer> sems = new ArrayList<>(history.keySet());
            int lastSem = sems.get(sems.size() - 1);
            int prevSem = sems.get(sems.size() - 2);
            double currentGPA = history.get(lastSem);
            double prevGPA = history.get(prevSem);

            if (currentGPA > prevGPA) {
                insights.add(String.format("📈 Upward Trend: GPA increased by %.2f in Sem %d.", (currentGPA - prevGPA),
                        lastSem));
            } else if (currentGPA < prevGPA) {
                insights.add(String.format("📉 Downward Trend: GPA dropped by %.2f in Sem %d.", (prevGPA - currentGPA),
                        lastSem));
            }
        }

        return insights;
    }

    private double calculateSemesterGPA(List<Course> courses) {
        double points = 0;
        double credits = 0;
        for (Course c : courses) {
            points += c.getGradePoints() * c.getCredits();
            credits += c.getCredits();
        }
        return (credits > 0) ? points / credits : 0.0;
    }

    private String extractCategory(String courseName, int credits) {
        if (credits == 2)
            return "Laboratory";
        if (courseName != null) {
            String upper = courseName.trim().toUpperCase();
            if (upper.contains("LAB") || upper.endsWith("LABORATORY"))
                return "Laboratory";
        }
        return "Theory";
    }
}
