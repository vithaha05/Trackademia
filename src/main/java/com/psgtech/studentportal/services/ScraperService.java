package com.psgtech.studentportal.services;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.utils.SessionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Scraper Service
 * Scrapes data from PSG Tech eCampus portal
 */
public class ScraperService {

    private SessionManager sessionManager;

    public ScraperService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Get course code to course name mapping
     */
    public Map<String, String> getCourseMap() throws IOException {
        System.out.println("📚 Fetching course mappings...");

        String coursesUrl = "https://ecampus.psgtech.ac.in/studzone/Attendance/courseplan";
        Document coursesPage = sessionManager.fetchStudzone1Page(coursesUrl);

        Map<String, String> courseMap = new HashMap<>();
        Elements courses = coursesPage.select("div.col-md-8");

        for (Element course : courses) {
            String courseCode = course.select("h5").text().trim();
            String courseName = course.select("h6").text().trim();

            // Extract initials from course name (uppercase letters)
            StringBuilder initials = new StringBuilder();
            for (String word : courseName.split(" ")) {
                if (!word.isEmpty() && Character.isUpperCase(word.charAt(0))) {
                    initials.append(word.charAt(0));
                }
            }

            courseMap.put(courseCode, initials.toString());
        }

        System.out.println("✅ Found " + courseMap.size() + " courses");
        return courseMap;
    }

    /**
     * Scrape internal marks (CA marks)
     */
    public List<InternalMarks> scrapeInternalMarks() throws IOException {
        System.out.println("📊 Scraping internal marks...");

        String internalsUrl = "https://ecampus.psgtech.ac.in/studzone/ContinuousAssessment/CAMarksView";
        Document internalsPage = sessionManager.fetchStudzone1Page(internalsUrl);

        List<InternalMarks> internalsList = new ArrayList<>();
        Map<String, String> courseMap = getCourseMap();

        Elements tables = internalsPage.select("table");
        if (tables.size() < 2) {
            System.out.println("⚠️ Internal marks not yet published");
            return internalsList;
        }

        Element theoryTable = tables.get(1);
        Elements rows = theoryTable.select("tbody tr");

        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 8) continue;

            InternalMarks internal = new InternalMarks();
            internal.setRollNo(sessionManager.getRollNo());

            String courseCode = cells.get(0).text().trim();
            internal.setCourseCode(courseCode);
            internal.setCourseName(courseMap.getOrDefault(courseCode, ""));

            // Parse internal marks (second to last column)
            try {
                String totalInternal = cells.get(cells.size() - 2).text().trim();
                if (!totalInternal.equals("*") && !totalInternal.isEmpty()) {
                    internal.setTotalInternalMarks(Double.parseDouble(totalInternal));
                }
            } catch (NumberFormatException e) {
                // Skip if marks are not numbers
            }

            internalsList.add(internal);
        }

        System.out.println("✅ Scraped " + internalsList.size() + " internal marks records");
        return internalsList;
    }

    /**
     * Scrape completed courses and grades
     */
    public List<Course> scrapeCompletedCourses() throws IOException {
        System.out.println("📚 Scraping completed courses...");

        String coursesUrl = "https://ecampus.psgtech.ac.in/studzone2/AttWfStudCourseSelection.aspx";
        Document coursesPage = sessionManager.fetchStudzone2Page(coursesUrl);

        List<Course> courses = new ArrayList<>();
        Element completedCoursesTable = coursesPage.select("table#PDGCourse").first();

        if (completedCoursesTable == null) {
            System.out.println("⚠️ No completed courses found");
            return courses;
        }

        Elements rows = completedCoursesTable.select("tr");

        // Grade mapping
        Map<String, Double> gradeMap = new HashMap<>();
        gradeMap.put("O", 10.0);
        gradeMap.put("A+", 9.0);
        gradeMap.put("A", 8.0);
        gradeMap.put("B+", 7.0);
        gradeMap.put("B", 6.0);
        gradeMap.put("C", 5.0);
        gradeMap.put("U", 0.0);

        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td");
            if (cells.size() < 8) continue;

            Course course = new Course();
            course.setRollNo(sessionManager.getRollNo());
            course.setCourseCode(cells.get(1).text().trim());
            course.setCourseName(cells.get(2).text().trim());

            try {
                course.setSemester(Integer.parseInt(cells.get(4).text().trim()));
                course.setCredits(Integer.parseInt(cells.get(5).text().trim()));
            } catch (NumberFormatException e) {
                continue;
            }

            String grade = cells.get(6).text().trim();
            course.setGrade(grade);
            course.setGradePoints(gradeMap.getOrDefault(grade, 0.0));

            courses.add(course);
        }

        System.out.println("✅ Scraped " + courses.size() + " courses");
        return courses;
    }

    /**
     * Calculate CGPA from courses
     */
    public List<CGPARecord> calculateCGPA(List<Course> courses, int completedSemester) {
        System.out.println("🧮 Calculating CGPA...");

        List<CGPARecord> cgpaRecords = new ArrayList<>();

        if (courses.isEmpty()) return cgpaRecords;

        int maxSemester = courses.stream()
                .mapToInt(Course::getSemester)
                .max()
                .orElse(1);

        double overallProduct = 0;
        int overallCredits = 0;
        boolean hasBacklogs = false;

        for (int semester = 1; semester <= maxSemester; semester++) {
            CGPARecord record = new CGPARecord();
            record.setRollNo(sessionManager.getRollNo());
            record.setSemester(semester);

            if (!hasBacklogs) {
                final int currentSem = semester;
                List<Course> semesterCourses = courses.stream()
                        .filter(c -> c.getSemester() == currentSem)
                        .toList();

                if (semester >= completedSemester) {
                    hasBacklogs = true;
                    record.setHasBacklogs(true);
                } else {
                    double semesterProduct = semesterCourses.stream()
                            .mapToDouble(c -> c.getGradePoints() * c.getCredits())
                            .sum();

                    int semesterCredits = semesterCourses.stream()
                            .mapToInt(Course::getCredits)
                            .sum();

                    overallProduct += semesterProduct;
                    overallCredits += semesterCredits;

                    double gpa = semesterCredits > 0 ? semesterProduct / semesterCredits : 0;
                    double cgpa = overallCredits > 0 ? overallProduct / overallCredits : 0;

                    record.setGpa(Math.round(gpa * 1000.0) / 1000.0);
                    record.setCgpa(Math.round(cgpa * 1000.0) / 1000.0);
                    record.setTotalCredits(overallCredits);
                    record.setHasBacklogs(false);
                }
            } else {
                record.setHasBacklogs(true);
            }

            cgpaRecords.add(record);
        }

        System.out.println("✅ Calculated CGPA for " + cgpaRecords.size() + " semesters");
        return cgpaRecords;
    }

    /**
     * Get completed semester (finds first semester with RA status)
     */
    public int getCompletedSemester() throws IOException {
        System.out.println("🔍 Finding completed semester...");

        String resultsUrl = "https://ecampus.psgtech.ac.in/studzone2/FrmEpsStudResult.aspx";
        Document resultsPage = sessionManager.fetchStudzone2Page(resultsUrl);

        Element resultsTable = resultsPage.select("table#DgResult").first();
        if (resultsTable == null) {
            System.out.println("⚠️ Results table not found, assuming semester 1");
            return 1;
        }

        Elements rows = resultsTable.select("tr");
        int lastSemester = 1;

        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td");
            if (cells.size() < 6) continue;

            String semText = cells.get(0).text().trim();
            if (!semText.isEmpty()) {
                try {
                    lastSemester = Integer.parseInt(semText);
                } catch (NumberFormatException e) {
                    // Skip invalid semester numbers
                }
            }

            String status = cells.get(5).text().trim();
            if (status.equals("RA")) {
                System.out.println("✅ Found RA status at semester " + lastSemester);
                return lastSemester;
            }
        }

        System.out.println("✅ All semesters completed, returning next semester: " + (lastSemester + 1));
        return lastSemester + 1;
    }
}