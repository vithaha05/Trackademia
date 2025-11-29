package com.psgtech.studentportal.services;

import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.database.DatabaseManager;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enhanced Scraper Service with Database Integration
 * Scrapes data from PSG Tech portal and saves to database
 */
public class ScraperService {

    private Map<String, String> cookies;
    private DatabaseService databaseService;
    private String currentRollNo;

    private static final String BASE_URL_ATTENDANCE = "https://ecampus.psgtech.ac.in/studzone";
    private static final String BASE_URL_CGPA = "https://ecampus.psgtech.ac.in/studzone2/";

    public ScraperService(DatabaseManager dbManager) {
        this.cookies = new HashMap<>();
        this.databaseService = new DatabaseService(dbManager);
    }

    /**
     * Complete login and data fetch operation
     */
    public boolean loginAndFetchData(String rollNo, String password) {
        this.currentRollNo = rollNo;

        System.out.println("🔄 Starting data scraping for: " + rollNo);

        // Login to both portals
        boolean attendanceLogin = loginToAttendancePortal(rollNo, password);
        boolean cgpaLogin = loginToCGPAPortal(rollNo, password);

        if (!attendanceLogin) {
            System.err.println("❌ Failed to login to attendance portal");
            return false;
        }

        if (!cgpaLogin) {
            System.err.println("❌ Failed to login to CGPA portal");
            return false;
        }

        System.out.println("✅ Successfully logged in to both portals");

        // Fetch and save data
        try {
            fetchAndSaveStudentInfo();
            fetchAndSaveInternalMarks();
            fetchAndSaveCourses();
            System.out.println("✅ All data scraped and saved successfully!");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error during data fetching: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Login to attendance portal
     */
    private boolean loginToAttendancePortal(String rollNo, String password) {
        try {
            Connection.Response loginPageResponse = Jsoup.connect(BASE_URL_ATTENDANCE)
                    .timeout(10000)
                    .method(Connection.Method.GET)
                    .execute();

            Document loginPage = loginPageResponse.parse();
            String token = loginPage.select("input[name=__RequestVerificationToken]").val();

            cookies.putAll(loginPageResponse.cookies());

            Map<String, String> loginData = new HashMap<>();
            loginData.put("rollno", rollNo);
            loginData.put("password", password);
            loginData.put("chkterms", "on");
            loginData.put("__RequestVerificationToken", token);

            Connection.Response loginResponse = Jsoup.connect(BASE_URL_ATTENDANCE)
                    .cookies(cookies)
                    .data(loginData)
                    .timeout(10000)
                    .method(Connection.Method.POST)
                    .execute();

            cookies.putAll(loginResponse.cookies());

            Document homePage = loginResponse.parse();
            Element navbar = homePage.selectFirst("nav.navbar.navbar-expand-lg.navbar-light");

            return navbar != null;

        } catch (IOException e) {
            System.err.println("❌ Login to attendance portal failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Login to CGPA portal
     */
    private boolean loginToCGPAPortal(String rollNo, String password) {
        try {
            Connection.Response loginPageResponse = Jsoup.connect(BASE_URL_CGPA)
                    .timeout(10000)
                    .method(Connection.Method.GET)
                    .execute();

            Document loginPage = loginPageResponse.parse();
            String viewstate = loginPage.select("input[name=__VIEWSTATE]").val();
            String viewstateGenerator = loginPage.select("input[name=__VIEWSTATEGENERATOR]").val();
            String eventValidation = loginPage.select("input[name=__EVENTVALIDATION]").val();
            String abcd3 = loginPage.select("input[name=abcd3]").val();

            Map<String, String> cgpaCookies = new HashMap<>(loginPageResponse.cookies());

            Map<String, String> loginData = new HashMap<>();
            loginData.put("__EVENTTARGET", "");
            loginData.put("__EVENTARGUMENT", "");
            loginData.put("__LASTFOCUS", "");
            loginData.put("__VIEWSTATE", viewstate);
            loginData.put("__VIEWSTATEGENERATOR", viewstateGenerator);
            loginData.put("__EVENTVALIDATION", eventValidation);
            loginData.put("rdolst", "S");
            loginData.put("txtusercheck", rollNo);
            loginData.put("txtpwdcheck", password);
            loginData.put("abcd3", abcd3);

            Connection.Response loginResponse = Jsoup.connect(BASE_URL_CGPA)
                    .cookies(cgpaCookies)
                    .data(loginData)
                    .timeout(10000)
                    .method(Connection.Method.POST)
                    .execute();

            cookies.putAll(loginResponse.cookies());

            return true;

        } catch (IOException e) {
            System.err.println("❌ Login to CGPA portal failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetch and save student basic info
     */
    private void fetchAndSaveStudentInfo() throws SQLException {
        try {
            String profileUrl = "https://ecampus.psgtech.ac.in/studzone/Home/Profile";
            Document page = Jsoup.connect(profileUrl)
                    .cookies(cookies)
                    .timeout(10000)
                    .get();

            Element profileName = page.selectFirst("h2.profile-name");
            String name = profileName != null ? profileName.text() : "Unknown";

            // Get current semester from courses page
            int currentSemester = getCompletedSemester();

            // Create student object
            Student student = new Student();
            student.setRollNo(currentRollNo);
            student.setName(name);
            student.setCurrentSemester(currentSemester);

            // Try to get additional info from scholarship page
            try {
                String scholarshipUrl = "https://ecampus.psgtech.ac.in/studzone/Scholar/VallalarScholarship";
                Document scholarshipPage = Jsoup.connect(scholarshipUrl)
                        .cookies(cookies)
                        .timeout(10000)
                        .get();

                Element personalInfoTable = scholarshipPage.selectFirst("td.personal-info");
                if (personalInfoTable != null) {
                    Elements personalInfo = personalInfoTable.select("td");

                    if (personalInfo.size() >= 3) {
                        String birthdateStr = personalInfo.get(2).text().trim();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        LocalDate birthdate = LocalDate.parse(birthdateStr, formatter);
                        student.setDateOfBirth(birthdate);
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not fetch birthday info");
            }

            databaseService.saveStudent(student);
            System.out.println("✅ Student info saved");

        } catch (IOException e) {
            System.err.println("❌ Failed to fetch student info: " + e.getMessage());
        }
    }

    /**
     * Fetch and save internal marks
     */
    private void fetchAndSaveInternalMarks() throws SQLException {
        try {
            String internalsUrl = "https://ecampus.psgtech.ac.in/studzone/ContinuousAssessment/CAMarksView";
            Document page = Jsoup.connect(internalsUrl)
                    .cookies(cookies)
                    .timeout(10000)
                    .get();

            Elements tables = page.select("table");

            if (tables.size() < 2) {
                System.err.println("❌ Internal marks tables not found");
                return;
            }

            Element theoryTable = tables.get(1);
            Element tbody = theoryTable.selectFirst("tbody");

            if (tbody == null) return;

            Elements rows = tbody.select("tr");
            int currentSemester = getCompletedSemester();

            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() < 2) continue;

                String courseCode = cells.get(0).text().trim();

                // Get the total marks (usually the last column with a valid number)
                String totalMarksStr = "";
                for (int i = cells.size() - 1; i >= 1; i--) {
                    String cellText = cells.get(i).text().trim();
                    if (!cellText.isEmpty() && !cellText.equals("*")) {
                        totalMarksStr = cellText;
                        break;
                    }
                }

                if (totalMarksStr.isEmpty()) continue;

                try {
                    double totalMarks = Double.parseDouble(totalMarksStr);

                    InternalMarks internal = new InternalMarks();
                    internal.setRollNo(currentRollNo);
                    internal.setSemester(currentSemester);
                    internal.setCourseCode(courseCode);
                    internal.setCourseName("Course " + courseCode);
                    internal.setTotalInternalMarks(totalMarks);
                    internal.setMaxMarks(50.0);

                    databaseService.saveInternalMarks(internal);

                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Could not parse marks for " + courseCode);
                }
            }

            System.out.println("✅ Internal marks saved");

        } catch (IOException e) {
            System.err.println("❌ Failed to fetch internal marks: " + e.getMessage());
        }
    }

    /**
     * Fetch and save courses with grades
     */
    private void fetchAndSaveCourses() throws SQLException {
        try {
            String coursesUrl = "https://ecampus.psgtech.ac.in/studzone2/AttWfStudCourseSelection.aspx";
            Document page = Jsoup.connect(coursesUrl)
                    .cookies(cookies)
                    .timeout(10000)
                    .get();

            Element completedCoursesTable = page.selectFirst("table#PDGCourse");

            if (completedCoursesTable == null) {
                System.err.println("❌ Completed courses table not found");
                return;
            }

            Elements rows = completedCoursesTable.select("tr");

            for (int i = 1; i < rows.size(); i++) {
                Elements cells = rows.get(i).select("td");
                if (cells.size() < 8) continue;

                try {
                    String courseCode = cells.get(1).text().trim();
                    String courseName = cells.get(2).text().trim();
                    int semester = Integer.parseInt(cells.get(4).text().trim());
                    int credits = Integer.parseInt(cells.get(5).text().trim());
                    String grade = cells.get(6).text().trim();
                    double gradePoints = convertLetterGradeToPoint(grade);

                    Course course = new Course();
                    course.setRollNo(currentRollNo);
                    course.setSemester(semester);
                    course.setCourseCode(courseCode);
                    course.setCourseName(courseName);
                    course.setCredits(credits);
                    course.setGrade(grade);
                    course.setGradePoints(gradePoints);

                    databaseService.saveCourse(course);

                } catch (Exception e) {
                    System.out.println("⚠️ Could not parse course at row " + i);
                }
            }

            // Calculate and save CGPA
            calculateAndSaveCGPA();

            System.out.println("✅ Courses and CGPA saved");

        } catch (IOException e) {
            System.err.println("❌ Failed to fetch courses: " + e.getMessage());
        }
    }

    /**
     * Calculate and save CGPA records
     */
    private void calculateAndSaveCGPA() throws SQLException {
        List<Course> courses = databaseService.getCourses(currentRollNo);

        if (courses.isEmpty()) return;

        // Group courses by semester
        Map<Integer, List<Course>> coursesBySemester = new HashMap<>();
        for (Course course : courses) {
            coursesBySemester.computeIfAbsent(course.getSemester(), k -> new ArrayList<>()).add(course);
        }

        // Calculate cumulative CGPA
        double totalGradePoints = 0;
        int totalCredits = 0;

        for (int sem = 1; sem <= Collections.max(coursesBySemester.keySet()); sem++) {
            List<Course> semCourses = coursesBySemester.get(sem);

            if (semCourses != null && !semCourses.isEmpty()) {
                double semGradePoints = 0;
                int semCredits = 0;

                for (Course course : semCourses) {
                    semGradePoints += course.getGradePoints() * course.getCredits();
                    semCredits += course.getCredits();
                }

                totalGradePoints += semGradePoints;
                totalCredits += semCredits;

                double semGPA = semCredits > 0 ? semGradePoints / semCredits : 0;
                double cgpa = totalCredits > 0 ? totalGradePoints / totalCredits : 0;

                CGPARecord record = new CGPARecord();
                record.setRollNo(currentRollNo);
                record.setSemester(sem);
                record.setGpa(semGPA);
                record.setCgpa(cgpa);
                record.setTotalCredits(totalCredits);
                record.setHasBacklogs(false);

                databaseService.saveCGPARecord(record);
            }
        }
    }

    /**
     * Get completed semester
     */
    private int getCompletedSemester() {
        try {
            String resultsUrl = "https://ecampus.psgtech.ac.in/studzone2/FrmEpsStudResult.aspx";
            Document page = Jsoup.connect(resultsUrl)
                    .cookies(cookies)
                    .timeout(10000)
                    .get();

            Element resultsTable = page.selectFirst("table#DgResult");

            if (resultsTable == null) {
                return 1;
            }

            Elements rows = resultsTable.select("tr");
            int lastSemester = 1;

            for (int i = 1; i < rows.size(); i++) {
                Elements cells = rows.get(i).select("td");
                if (cells.size() < 6) continue;

                String semesterText = cells.get(0).text().trim();
                if (!semesterText.isEmpty()) {
                    lastSemester = Integer.parseInt(semesterText);
                }

                if (cells.get(5).text().trim().equals("RA")) {
                    return lastSemester;
                }
            }

            return lastSemester + 1;

        } catch (Exception e) {
            System.err.println("❌ Failed to get completed semester: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Convert letter grade to grade point
     */
    private double convertLetterGradeToPoint(String grade) {
        Map<String, Double> gradeMap = new HashMap<>();
        gradeMap.put("O", 10.0);
        gradeMap.put("A+", 9.0);
        gradeMap.put("A", 8.0);
        gradeMap.put("B+", 7.0);
        gradeMap.put("B", 6.0);
        gradeMap.put("C", 5.0);

        return gradeMap.getOrDefault(grade, 0.0);
    }

    /**
     * Calculate target score for desired final marks
     */
    public double calculateTargetScore(double internalMarks, double desiredFinalMarks) {
        double target = (desiredFinalMarks - 0.8 * internalMarks) / 0.6;

        if (target > 100) {
            return -1;
        } else if (target > 45) {
            return Math.ceil(target);
        } else {
            return 45;
        }
    }

    /**
     * Get student greeting with birthday check
     */
    public String getStudentGreeting() {
        try {
            String scholarshipUrl = "https://ecampus.psgtech.ac.in/studzone/Scholar/VallalarScholarship";
            Document page = Jsoup.connect(scholarshipUrl)
                    .cookies(cookies)
                    .timeout(10000)
                    .get();

            Element personalInfoTable = page.selectFirst("td.personal-info");
            if (personalInfoTable == null) {
                return fallbackGreeting();
            }

            Elements personalInfo = personalInfoTable.select("td");
            String username = personalInfo.get(0).text().trim();
            String birthdateStr = personalInfo.get(2).text().trim();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate birthdate = LocalDate.parse(birthdateStr, formatter);
            LocalDate today = LocalDate.now();

            if (birthdate.getMonth() == today.getMonth() &&
                    birthdate.getDayOfMonth() == today.getDayOfMonth()) {
                return "Happy Birthday " + username + "! 🎉";
            } else {
                return "Welcome " + username + "!";
            }

        } catch (Exception e) {
            return fallbackGreeting();
        }
    }

    private String fallbackGreeting() {
        try {
            String profileUrl = "https://ecampus.psgtech.ac.in/studzone/Home/Profile";
            Document page = Jsoup.connect(profileUrl)
                    .cookies(cookies)
                    .timeout(10000)
                    .get();

            Element profileName = page.selectFirst("h2.profile-name");
            if (profileName != null) {
                return "Welcome " + profileName.text() + "!";
            }
        } catch (IOException e) {
            System.err.println("⚠️ Fallback greeting failed");
        }

        return "Welcome Student!";
    }
}