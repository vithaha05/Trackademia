package com.psgtech.studentportal.services;

import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.database.DatabaseManager;
import com.psgtech.studentportal.utils.SessionManager;
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

    private final SessionManager sessionManager;
    private DatabaseService databaseService;
    private String currentRollNo;

    private static final String BASE_URL_ATTENDANCE = "https://ecampus.psgtech.ac.in/studzone";
    private static final String BASE_URL_CGPA = "https://ecampus.psgtech.ac.in/studzone2/";

    public ScraperService(DatabaseManager dbManager) {
        this.sessionManager = SessionManager.getInstance();
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
            fetchAndSaveStudentInfo();
            fetchAndSaveCourses(); // Fetch courses first to match course names
            fetchAndSaveInternalMarks(); // Then fetch internal marks with proper course names
            fetchAndSaveAttendance(); // Fetch attendance
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
            return sessionManager.loginToStudzone1(rollNo, password);
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
            return sessionManager.loginToStudzone2(rollNo, password);
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
            Document page = sessionManager.fetchStudzone1Page(profileUrl);

            Element profileName = page.selectFirst("h2.profile-name");
            String name = profileName != null ? profileName.text() : "Unknown";

            // Get current semester from courses page
            int currentSemester = getCompletedSemester();

            // Create student object
            Student student = new Student();
            student.setRollNo(currentRollNo);
            student.setName(name);
            student.setCurrentSemester(currentSemester);

            // Fetch program info from attendance page
            try {
                String attendanceUrl = "https://ecampus.psgtech.ac.in/studzone/Attendance/StudentPercentage";
                Document attendancePage = sessionManager.fetchStudzone1Page(attendanceUrl);

                String program = "";

                // Debug: Print page structure
                System.out.println("🔍 Searching for program info on attendance page...");

                // Method 1: Look for common label patterns
                Elements allElements = attendancePage.select("td, th, span, label, div");
                for (Element el : allElements) {
                    String text = el.text().trim();
                    // Check if this element contains program keywords
                    if (text.toUpperCase().contains("M.SC") || text.toUpperCase().contains("MSC") ||
                            text.toUpperCase().contains("THEORETICAL") || text.toUpperCase().contains("CYBER") ||
                            text.toUpperCase().contains("DATA SCIENCE")
                            || text.toUpperCase().contains("SOFTWARE SYSTEMS")) {
                        System.out.println("🔍 Found potential program text: " + text);
                        if (program.isEmpty()) {
                            program = extractProgramFromText(text);
                        }
                    }
                }

                // Method 2: Check the entire page body for program keywords
                if (program.isEmpty()) {
                    String bodyText = attendancePage.body().text();
                    System.out.println("🔍 Searching full page text for program...");

                    // Look for specific program names
                    String[] programPatterns = {
                            "M.Sc. Theoretical Computer Science",
                            "M.Sc Theoretical Computer Science",
                            "MSc Theoretical Computer Science",
                            "M.Sc. Cyber Security",
                            "M.Sc. Data Science",
                            "M.Sc. Software Systems",
                            "B.E. Computer Science",
                            "B.Tech"
                    };

                    for (String pattern : programPatterns) {
                        if (bodyText.toLowerCase().contains(pattern.toLowerCase())) {
                            program = pattern;
                            System.out.println("🔍 Found program in body text: " + program);
                            break;
                        }
                    }
                }

                // Method 3: If still empty, try to infer from roll number
                if (program.isEmpty()) {
                    program = inferProgramFromRollNo(currentRollNo);
                    System.out.println("🔍 Inferred from roll number: " + program);
                }

                if (!program.isEmpty()) {
                    student.setProgram(program);
                    int totalSemesters = determineTotalSemesters(program);
                    student.setTotalSemesters(totalSemesters);
                    System.out.println("✅ Program detected: " + program + " (" + totalSemesters + " semesters)");
                } else {
                    // Default to 8 semesters for engineering
                    student.setTotalSemesters(8);
                    System.out.println("⚠️ Could not detect program, defaulting to 8 semesters");
                }

            } catch (Exception e) {
                System.out.println("⚠️ Could not fetch program info: " + e.getMessage());
                student.setTotalSemesters(8); // Default
            }

            // Try to get additional info from scholarship page
            try {
                String scholarshipUrl = "https://ecampus.psgtech.ac.in/studzone/Scholar/VallalarScholarship";
                Document scholarshipPage = sessionManager.fetchStudzone1Page(scholarshipUrl);

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
     * Extract program name from text
     */
    private String extractProgramFromText(String text) {
        // Try specific program patterns first (more precise)
        String[] specificPatterns = {
                "MSc\\s+CYBER\\s+SECURITY",
                "MSc\\s+THEORETICAL\\s+COMPUTER\\s+SCIENCE",
                "MSc\\s+DATA\\s+SCIENCE",
                "MSc\\s+SOFTWARE\\s+SYSTEMS",
                "M\\.Sc\\.?\\s+[A-Za-z\\s]+",
                "B\\.E\\.?\\s+[A-Za-z\\s]+",
                "B\\.Tech\\.?\\s+[A-Za-z\\s]+"
        };

        for (String pattern : specificPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern,
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                String found = m.group().trim();
                // Normalize MSc to M.Sc.
                if (found.toUpperCase().startsWith("MSC ")) {
                    found = "M.Sc." + found.substring(3);
                }
                System.out.println("🔍 Extracted program: " + found);
                return found;
            }
        }
        return "";
    }

    /**
     * Infer program from roll number pattern
     * PSG Tech roll number format: YYDCXX where YY=year, DC=dept code, XX=number
     * Examples: 23PT40 = 2023 batch, PT = Theoretical CS, 40 = roll number
     */
    private String inferProgramFromRollNo(String rollNo) {
        if (rollNo == null || rollNo.length() < 4)
            return "";

        String prefix = rollNo.substring(2, 4).toUpperCase();
        System.out.println("🔍 Inferring program from roll prefix: " + prefix);

        // MSc Integrated programs at PSG Tech (5-year, 10 semesters)
        switch (prefix) {
            case "PT":
                return "M.Sc. Theoretical Computer Science";
            case "PC":
                return "M.Sc. Cyber Security";
            case "PD":
                return "M.Sc. Data Science";
            case "PW":
                return "M.Sc. Software Systems";
            // Add more MSc department codes as needed
        }

        // Common B.E./B.Tech department codes
        if (prefix.startsWith("C") || prefix.equals("CS"))
            return "B.E. Computer Science";
        if (prefix.startsWith("E") || prefix.equals("EC"))
            return "B.E. Electronics";
        if (prefix.equals("IT"))
            return "B.Tech Information Technology";
        if (prefix.startsWith("M") || prefix.equals("ME"))
            return "B.E. Mechanical";

        // Default fallback
        return "B.E. (Inferred)";
    }

    /**
     * Determine total semesters based on program type
     */
    private int determineTotalSemesters(String program) {
        if (program == null || program.isEmpty())
            return 8;

        String upperProgram = program.toUpperCase();

        // MSc Applied Mathematics - 4 semesters (2-year program) - check first
        if (upperProgram.contains("M.SC") && upperProgram.contains("APPLIED MATH")) {
            return 4;
        }

        // MSc Integrated 5-year programs - 10 semesters
        // Includes: Theoretical Computer Science, Cyber Security, Data Science,
        // Software Systems
        if (upperProgram.contains("M.SC") || upperProgram.contains("MSC")) {
            // Check for specific 5-year integrated programs
            if (upperProgram.contains("THEORETICAL") ||
                    upperProgram.contains("CYBER") ||
                    upperProgram.contains("DATA SCIENCE") ||
                    upperProgram.contains("SOFTWARE") ||
                    upperProgram.contains("INTEGRATED") ||
                    upperProgram.contains("5 YEAR") ||
                    upperProgram.contains("5-YEAR")) {
                return 10;
            }
            // If it's an MSc at PSG Tech (not Applied Math), it's likely integrated
            // Default MSc to 10 semesters
            return 10;
        }

        // MCA / MBA - typically 4 semesters
        if (upperProgram.contains("MCA") || upperProgram.contains("MBA")) {
            return 4;
        }

        // M.Tech / M.E. - typically 4 semesters
        if (upperProgram.contains("M.TECH") || upperProgram.contains("M.E")) {
            return 4;
        }

        // BE / BTech - 8 semesters (default for engineering)
        if (upperProgram.contains("B.E") || upperProgram.contains("B.TECH") ||
                upperProgram.contains("INFERRED")) {
            return 8;
        }

        // Default to 8 semesters
        return 8;
    }

    /**
     * Fetch and save internal marks
     * Note: Internal marks are only for the current semester
     */
    private void fetchAndSaveInternalMarks() throws SQLException {
        try {
            // Get current semester from student record
            Student student = databaseService.getStudent(currentRollNo);
            if (student == null) {
                System.err.println("❌ Student record not found, cannot determine current semester");
                return;
            }

            int currentSemester = student.getCurrentSemester();
            System.out.println("📊 Fetching internal marks for current semester: " + currentSemester);

            String internalsUrl = "https://ecampus.psgtech.ac.in/studzone/ContinuousAssessment/CAMarksView";
            Document page = sessionManager.fetchStudzone1Page(internalsUrl);

            Elements tables = page.select("table");

            if (tables.size() < 2) {
                System.err.println("❌ Internal marks tables not found");
                return;
            }

            Element theoryTable = tables.get(1);
            Element tbody = theoryTable.selectFirst("tbody");

            if (tbody == null)
                return;

            Elements rows = tbody.select("tr");
            int savedCount = 0;

            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() < 2)
                    continue;

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

                if (totalMarksStr.isEmpty())
                    continue;

                try {
                    double totalMarks = Double.parseDouble(totalMarksStr);

                    // Get actual course name from courses table
                    String courseName = databaseService.getCourseNameByCode(currentRollNo, courseCode);
                    if (courseName == null || courseName.isEmpty()) {
                        // Fallback if course not found
                        courseName = "Course " + courseCode;
                        System.out.println("⚠️ Course name not found for " + courseCode + ", using fallback");
                    }

                    InternalMarks internal = new InternalMarks();
                    internal.setRollNo(currentRollNo);
                    internal.setSemester(currentSemester); // Use current semester
                    internal.setCourseCode(courseCode);
                    internal.setCourseName(courseName); // Use actual course name
                    internal.setTotalInternalMarks(totalMarks);
                    internal.setMaxMarks(50.0);

                    databaseService.saveInternalMarks(internal);
                    savedCount++;

                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Could not parse marks for " + courseCode);
                }
            }

            System.out.println(
                    "✅ Internal marks saved for semester " + currentSemester + " (" + savedCount + " courses)");

        } catch (IOException e) {
            System.err.println("❌ Failed to fetch internal marks: " + e.getMessage());
        }
    }

    /**
     * Fetch and save attendance percentage
     */
    private void fetchAndSaveAttendance() throws SQLException {
        try {
            System.out.println("📊 Fetching attendance data...");
            String attendanceUrl = "https://ecampus.psgtech.ac.in/studzone/Attendance/StudentPercentage";
            Document page = sessionManager.fetchStudzone1Page(attendanceUrl);

            // The attendance page usually has a table with course code, name, and
            // percentage
            Elements tables = page.select("table");
            if (tables.isEmpty()) {
                System.err.println("❌ No tables found on attendance page");
                return;
            }

            // Iterate through tables to find the one with attendance data
            // Usually it's the second table or one with specific headers
            for (Element table : tables) {
                Elements rows = table.select("tr");
                if (rows.size() < 2)
                    continue;

                // Check header to verify it's the attendance table
                String headerText = rows.get(0).text().toLowerCase();
                if (!headerText.contains("code") || !headerText.contains("perc")) {
                    continue;
                }

                int savedCount = 0;
                for (int i = 1; i < rows.size(); i++) {
                    Elements cells = rows.get(i).select("td");
                    if (cells.size() < 3)
                        continue;

                    String courseCode = cells.get(0).text().trim();
                    // Some tables might have S.No as first column
                    if (courseCode.matches("\\d+") && cells.size() > 3) {
                        courseCode = cells.get(1).text().trim();
                    }

                    // Find the percentage column (usually the last or near last)
                    // Look for cell with % symbol
                    String percentageStr = "";
                    for (int j = cells.size() - 1; j >= 0; j--) {
                        String text = cells.get(j).text().trim();
                        if (text.endsWith("%") || text.matches("\\d+(\\.\\d+)?")) {
                            // verify it's a number
                            String numPart = text.replace("%", "").trim();
                            if (numPart.matches("\\d+(\\.\\d+)?")) {
                                percentageStr = numPart;
                                break;
                            }
                        }
                    }

                    if (!percentageStr.isEmpty() && !courseCode.isEmpty()) {
                        try {
                            double percentage = Double.parseDouble(percentageStr);
                            databaseService.updateAttendance(currentRollNo, courseCode, percentage);
                            savedCount++;
                            System.out.println("✅ Attendance for " + courseCode + ": " + percentage + "%");
                        } catch (NumberFormatException e) {
                            // Ignore parse errors
                        }
                    }
                }

                if (savedCount > 0) {
                    System.out.println("✅ Saved attendance for " + savedCount + " courses");
                    return; // Stop after finding the valid table
                }
            }
            System.out.println("⚠️ Could not find valid attendance data in any table");

        } catch (IOException e) {
            System.err.println("❌ Failed to fetch attendance: " + e.getMessage());
        }
    }

    /**
     * Fetch and save courses with grades - ENHANCED WITH DEBUGGING
     */
    private void fetchAndSaveCourses() throws SQLException {
        try {
            String coursesUrl = "https://ecampus.psgtech.ac.in/studzone2/AttWfStudCourseSelection.aspx";
            System.out.println("🔄 Fetching courses page...");
            Document page = sessionManager.fetchStudzone2Page(coursesUrl);

            Element completedCoursesTable = page.selectFirst("table#PDGCourse");

            if (completedCoursesTable == null) {
                System.err.println("❌ Completed courses table not found");
                return;
            }

            Elements rows = completedCoursesTable.select("tr");
            System.out.println("📊 Total rows in table: " + rows.size());

            // Debug: Print first row structure
            if (rows.size() > 0) {
                Elements headerCells = rows.get(0).select("th, td");
                System.out.println("📋 Header row has " + headerCells.size() + " columns:");
                for (int i = 0; i < headerCells.size(); i++) {
                    System.out.println("  [" + i + "] " + headerCells.get(i).text());
                }
            }

            // Debug: Print first data row
            if (rows.size() > 1) {
                Elements firstRowCells = rows.get(1).select("td");
                System.out.println("📝 First data row has " + firstRowCells.size() + " cells:");
                for (int i = 0; i < firstRowCells.size(); i++) {
                    System.out.println("  [" + i + "] '" + firstRowCells.get(i).text() + "'");
                }
            }

            int successCount = 0;
            int failCount = 0;

            for (int i = 1; i < rows.size(); i++) {
                Elements cells = rows.get(i).select("td");

                // More flexible cell count check
                if (cells.size() < 7) {
                    System.out.println("⚠️ Row " + i + " has only " + cells.size() + " cells, skipping");
                    failCount++;
                    continue;
                }

                try {
                    // Try to parse with flexible indexing
                    String courseCode = "";
                    String courseName = "";
                    String semesterText = "";
                    String creditsText = "";
                    String grade = "";

                    // Parse based on actual table structure
                    // [0]=S.No, [1]=Code, [2]=Name, [3]=Category, [4]=Sem, [5]=Option, [6]=Grade,
                    // [7]=Credits, [8]=Year
                    if (cells.size() >= 8) {
                        courseCode = cells.get(1).text().trim();
                        courseName = cells.get(2).text().trim();
                        semesterText = cells.get(4).text().trim();
                        grade = cells.get(6).text().trim();
                        creditsText = cells.get(7).text().trim(); // FIXED: Credits is column 7, not 5!
                    } else if (cells.size() >= 7) {
                        // Fallback format
                        courseCode = cells.get(1).text().trim();
                        courseName = cells.get(2).text().trim();
                        semesterText = cells.get(4).text().trim();
                        creditsText = cells.get(5).text().trim();
                        grade = cells.get(6).text().trim();
                    } else if (cells.size() >= 6) {
                        // Alternative format without S.No
                        courseCode = cells.get(0).text().trim();
                        courseName = cells.get(1).text().trim();
                        semesterText = cells.get(3).text().trim();
                        creditsText = cells.get(4).text().trim();
                        grade = cells.get(5).text().trim();
                    }

                    // Skip empty rows
                    if (courseCode.isEmpty() || courseName.isEmpty()) {
                        failCount++;
                        continue;
                    }

                    // Validate semester
                    if (!semesterText.matches("\\d+")) {
                        System.out
                                .println("⚠️ Row " + i + ": Invalid semester '" + semesterText + "' for " + courseCode);
                        failCount++;
                        continue;
                    }
                    int semester = Integer.parseInt(semesterText);

                    // Validate credits
                    if (!creditsText.matches("\\d+")) {
                        System.out.println("⚠️ Row " + i + ": Invalid credits '" + creditsText + "' for " + courseCode);
                        failCount++;
                        continue;
                    }
                    int credits = Integer.parseInt(creditsText);

                    // Validate grade
                    if (grade.isEmpty()) {
                        System.out.println("⚠️ Row " + i + ": Empty grade for " + courseCode);
                        failCount++;
                        continue;
                    }

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
                    successCount++;

                    if (successCount <= 3) {
                        System.out.println("✅ Saved: " + courseCode + " | Sem:" + semester + " | Credits:" + credits
                                + " | Grade:" + grade);
                    }

                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Row " + i + ": Number format error - " + e.getMessage());
                    failCount++;
                } catch (Exception e) {
                    System.out.println("⚠️ Row " + i + ": Parse error - " + e.getMessage());
                    failCount++;
                }
            }

            System.out.println("📊 Course parsing results: " + successCount + " success, " + failCount + " failed");

            // Calculate and save CGPA
            if (successCount > 0) {
                calculateAndSaveCGPA();
            }

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

        if (courses.isEmpty())
            return;

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
     * Get current semester based on multiple data sources
     * Priority: 1. Internal marks semester, 2. Highest course semester, 3. Results
     * page
     */
    private int getCompletedSemester() {
        try {
            // Priority 1: Check Results Page (Source of truth for completed semesters)
            // If results for Sem 5 are out, then Sem 5 is completed. Current is 6.
            int maxResultSem = getMaxSemesterFromResults();
            System.out.println("📊 Max semester from results page: " + maxResultSem);

            // Priority 2: Check Internal Marks Page (Source of truth for CURRENT ongoing
            // semester)
            // If marks for Sem 6 are visible, we are definitely in Sem 6.
            int internalMarksSem = getInternalMarksSemester();
            System.out.println("📊 Semester from internal marks: " + internalMarksSem);

            if (internalMarksSem > 0) {
                // If we see internal marks for Sem X, we are in Sem X.
                return internalMarksSem;
            }

            if (maxResultSem > 0) {
                // If internal marks not yet up, but results for Sem X are out,
                // we are likely in Sem X+1.
                return maxResultSem + 1;
            }

            // Priority 3: Fallback to database (only if scraping both failed)
            List<Course> courses = databaseService.getCourses(currentRollNo);
            if (courses != null && !courses.isEmpty()) {
                int maxCourseSem = courses.stream()
                        .mapToInt(Course::getSemester)
                        .max()
                        .orElse(0);
                if (maxCourseSem > 0) {
                    System.out.println("📊 Max completed semester from DB history: " + maxCourseSem);
                    // Assume next semester is current
                    return maxCourseSem + 1;
                }
            }

            return 1; // Default fallback

        } catch (Exception e) {
            System.err.println("❌ Failed to get completed semester: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Get max semester from results page
     */
    private int getMaxSemesterFromResults() {
        try {
            String resultsUrl = "https://ecampus.psgtech.ac.in/studzone2/FrmEpsStudResult.aspx";
            Document page = sessionManager.fetchStudzone2Page(resultsUrl);

            Element resultsTable = page.selectFirst("table#DgResult");
            if (resultsTable == null)
                return 0;

            Elements rows = resultsTable.select("tr");
            int maxSem = 0;

            for (int i = 1; i < rows.size(); i++) {
                Elements cells = rows.get(i).select("td");
                if (cells.size() > 0) {
                    String semesterText = cells.get(0).text().trim();
                    if (semesterText.matches("\\d+")) {
                        int sem = Integer.parseInt(semesterText);
                        if (sem > maxSem)
                            maxSem = sem;
                    }
                }
            }
            return maxSem;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get semester from internal marks page
     */
    private int getInternalMarksSemester() {
        try {
            String internalsUrl = "https://ecampus.psgtech.ac.in/studzone/ContinuousAssessment/CAMarksView";
            Document page = sessionManager.fetchStudzone1Page(internalsUrl);

            // Look for semester information in the page
            // The internal marks page typically shows current semester data
            Elements tables = page.select("table");

            if (tables.size() >= 2) {
                Element theoryTable = tables.get(1);
                Element tbody = theoryTable.selectFirst("tbody");

                if (tbody != null) {
                    Elements rows = tbody.select("tr");
                    if (rows.size() > 0) {
                        // If internal marks exist, check course codes to determine semester
                        Element firstRow = rows.first();
                        Elements cells = firstRow.select("td");
                        if (cells.size() > 0) {
                            String courseCode = cells.get(0).text().trim();
                            // Course codes like 23XT51 indicate semester 5
                            if (courseCode.length() >= 5) {
                                String semDigit = courseCode.substring(4, 5);
                                if (semDigit.matches("\\d")) {
                                    return Integer.parseInt(semDigit);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail, will use fallback
        }
        return 0;
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
        gradeMap.put("U", 0.0);
        gradeMap.put("RA", 0.0);

        return gradeMap.getOrDefault(grade, 0.0);
    }

    /**
     * Scrape completed courses and grades
     */
    public List<Course> scrapeCompletedCourses() throws IOException {
        System.out.println("📚 Scraping completed courses...");

        try {
            // Add delay to ensure studzone2 session is ready
            Thread.sleep(2000);

            String coursesUrl = "https://ecampus.psgtech.ac.in/studzone2/AttWfStudCourseSelection.aspx";

            // First GET request to load the page
            System.out.println("🔄 Fetching courses page...");
            Document coursesPage = sessionManager.fetchStudzone2Page(coursesUrl);

            // Check if we got redirected to login (session expired)
            if (coursesPage.select("input[name=txtusercheck]").size() > 0) {
                System.err.println("❌ Session expired, need to re-login");
                throw new IOException("Session expired on studzone2");
            }

            List<Course> courses = new ArrayList<>();

            // Look for the completed courses table
            Element completedCoursesTable = coursesPage.select("table#PDGCourse").first();

            if (completedCoursesTable == null) {
                // Try alternative selectors
                System.out.println("⚠️ Table #PDGCourse not found, trying alternatives...");

                Elements allTables = coursesPage.select("table");
                System.out.println("📊 Found " + allTables.size() + " tables on page:");
                for (int i = 0; i < allTables.size(); i++) {
                    Element table = allTables.get(i);
                    String tableId = table.attr("id");
                    String tableClass = table.attr("class");
                    System.out.println("  Table " + i + ": id='" + tableId + "', class='" + tableClass + "'");

                    if (tableId.contains("PDG") || tableId.contains("Course") ||
                            tableClass.contains("course") || tableClass.contains("grid")) {
                        completedCoursesTable = table;
                        System.out.println("  ✅ Using this table!");
                        break;
                    }
                }

                if (completedCoursesTable == null) {
                    System.err.println("❌ Could not find courses table on page");
                    return courses;
                }
            }

            Elements rows = completedCoursesTable.select("tr");
            System.out.println("📋 Found " + rows.size() + " rows in table");

            if (rows.size() <= 1) {
                System.out.println("⚠️ No data rows found (only header)");
                return courses;
            }

            // Grade mapping
            Map<String, Double> gradeMap = new HashMap<>();
            gradeMap.put("O", 10.0);
            gradeMap.put("A+", 9.0);
            gradeMap.put("A", 8.0);
            gradeMap.put("B+", 7.0);
            gradeMap.put("B", 6.0);
            gradeMap.put("C", 5.0);
            gradeMap.put("U", 0.0);
            gradeMap.put("RA", 0.0);

            for (int i = 1; i < rows.size(); i++) {
                Elements cells = rows.get(i).select("td");

                if (cells.size() < 7) {
                    continue;
                }

                try {
                    String courseCode = cells.get(1).text().trim();
                    String courseName = cells.get(2).text().trim();

                    if (courseCode.isEmpty() && courseName.isEmpty()) {
                        continue;
                    }

                    Course course = new Course();
                    course.setRollNo(sessionManager.getRollNo());
                    course.setCourseCode(courseCode);
                    course.setCourseName(courseName);

                    String semesterText = cells.get(4).text().trim();
                    if (!semesterText.isEmpty() && semesterText.matches("\\d+")) {
                        course.setSemester(Integer.parseInt(semesterText));
                    } else {
                        continue;
                    }

                    String creditsText = cells.get(5).text().trim();
                    if (!creditsText.isEmpty() && creditsText.matches("\\d+")) {
                        course.setCredits(Integer.parseInt(creditsText));
                    } else {
                        continue;
                    }

                    String grade = cells.get(6).text().trim();
                    course.setGrade(grade);
                    course.setGradePoints(gradeMap.getOrDefault(grade, 0.0));

                    courses.add(course);

                } catch (Exception e) {
                    // Silently skip problematic rows
                }
            }

            System.out.println("✅ Successfully scraped " + courses.size() + " courses");
            return courses;

        } catch (InterruptedException e) {
            System.err.println("❌ Thread interrupted");
            throw new IOException("Thread interrupted", e);
        }
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
            Document page = sessionManager.fetchStudzone1Page(scholarshipUrl);

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
            Document page = sessionManager.fetchStudzone1Page(profileUrl);

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