package com.campus.tracker.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.*;

public class ECampusScraper implements AutoCloseable {
    private WebDriver driver;
    private WebDriverWait wait;

    public ECampusScraper() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        options.addArguments("--no-sandbox", "--disable-gpu", "--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public Map<String, Object> loginAndScrape(String username, String password) {
        Map<String, Object> data = new HashMap<>();
        try {
            System.out.println("Opening old eCampus...");
            driver.get("https://ecampus.psgtech.ac.in/studzone2/");
            Thread.sleep(1500);

            // Login
            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("txtusercheck")));
            userField.sendKeys(username);
            System.out.println("✓ Username entered");

            WebElement passField = driver.findElement(By.id("txtpwdcheck"));
            passField.sendKeys(password);
            System.out.println("✓ Password entered");

            WebElement loginBtn = driver.findElement(By.id("abcd3"));
            loginBtn.click();
            System.out.println("✓ Login button clicked");

            // Check for alert
            try {
                Thread.sleep(800);
                Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                alert.accept();
                throw new RuntimeException(alertText);
            } catch (NoAlertPresentException e) {
                // Success
            }

            wait.until(ExpectedConditions.urlContains("AttWfStudMenu.aspx"));
            Thread.sleep(1000);
            System.out.println("✓ Login successful!");

            data.put("name", scrapeName());
            data.put("rollNo", username);
            data.put("programme", scrapeProgramme());

            // Get all courses grouped by semester from CGPA page
            Map<String, Object> semesterData = scrapeAllSemesterResults();
            data.put("examResults", semesterData.get("allCourses"));
            data.put("semesterGPAs", semesterData.get("semesterGPAs"));
            data.put("overallCGPA", semesterData.get("overallCGPA"));

            return data;

        } catch (Exception e) {
            System.err.println("Old eCampus error: " + e.getMessage());
            throw new RuntimeException("Old eCampus login failed: " + e.getMessage(), e);
        }
    }

    private String scrapeName() {
        try {
            return driver.findElement(By.id("Title1_LblStaffName")).getText().trim().split("\\(")[0].trim();
        } catch (Exception e) {
            return "Student";
        }
    }

    private String scrapeProgramme() {
        try {
            driver.get("https://ecampus.psgtech.ac.in/studzone2/AttWfStudCourseSelection.aspx");
            Thread.sleep(1500);
            return driver.findElement(By.id("TxtProgramme")).getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> scrapeAllSemesterResults() {
        List<Map<String, String>> allCourses = new ArrayList<>();
        Map<String, List<Map<String, String>>> semesterMap = new TreeMap<>();

        try {
            System.out.println("Navigating to CGPA page (ALL courses)...");
            driver.get("https://ecampus.psgtech.ac.in/studzone2/EpsWfStudCGPA.aspx");
            Thread.sleep(2000);

            // Wait for the main results table
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("DgResult")));

            List<WebElement> rows = driver.findElements(By.cssSelector("#DgResult tr"));
            System.out.println("Found " + rows.size() + " course rows");

            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<WebElement> cells = rows.get(i).findElements(By.tagName("td"));
                    if (cells.size() < 5) continue;

                    String semester = cells.get(0).getText().trim();
                    String courseCode = cells.get(1).getText().trim();
                    String courseTitle = cells.get(2).getText().trim();
                    String credits = cells.get(3).getText().trim();
                    String grade = cells.get(4).getText().trim();

                    // Skip header or empty rows
                    if (courseCode.isEmpty() ||
                            courseCode.contains("Course Code") ||
                            courseCode.contains("COURSE CODE") ||
                            semester.isEmpty() ||
                            semester.equals("Sem")) {
                        continue;
                    }

                    if (grade.isEmpty() || grade.equals("-")) continue;

                    Map<String, String> course = new HashMap<>();
                    course.put("semester", semester);
                    course.put("courseCode", courseCode);
                    course.put("courseTitle", courseTitle);
                    course.put("credit", credits);
                    course.put("grade", grade);
                    course.put("result", "Pass");

                    allCourses.add(course);

                    // Group by semester
                    semesterMap.putIfAbsent(semester, new ArrayList<>());
                    semesterMap.get(semester).add(course);

                } catch (Exception e) {
                    continue;
                }
            }

            System.out.println("Total courses: " + allCourses.size());
            System.out.println("Semesters found: " + semesterMap.keySet());

            // Calculate GPAs
            List<Map<String, Object>> semesterGPAs = calculateSemesterGPAs(semesterMap);
            double overallCGPA = calculateOverallCGPA(semesterGPAs);

            Map<String, Object> result = new HashMap<>();
            result.put("allCourses", allCourses);
            result.put("semesterGPAs", semesterGPAs);
            result.put("overallCGPA", overallCGPA);

            return result;

        } catch (Exception e) {
            System.err.println("Error scraping results: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> result = new HashMap<>();
            result.put("allCourses", allCourses);
            result.put("semesterGPAs", new ArrayList<>());
            result.put("overallCGPA", 0.0);
            return result;
        }
    }

    private List<Map<String, Object>> calculateSemesterGPAs(Map<String, List<Map<String, String>>> semesterMap) {
        List<Map<String, Object>> semesterGPAs = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, String>>> entry : semesterMap.entrySet()) {
            String semester = entry.getKey();
            List<Map<String, String>> courses = entry.getValue();

            double totalGradePoints = 0;
            int totalCredits = 0;

            for (Map<String, String> course : courses) {
                try {
                    String grade = course.get("grade");
                    int credits = Integer.parseInt(course.get("credit"));
                    double gradePoint = convertGradeToPoint(grade);

                    totalGradePoints += (gradePoint * credits);
                    totalCredits += credits;

                    System.out.println("  • " + course.get("courseCode") + " (" + credits + " credits) - Grade: " + grade + " = " + gradePoint + " points");
                } catch (Exception e) {
                    System.err.println("  ✗ Error processing: " + course.get("courseCode"));
                    continue;
                }
            }

            double gpa = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;

            Map<String, Object> semesterData = new HashMap<>();
            semesterData.put("semester", semester);
            semesterData.put("gpa", Math.round(gpa * 100.0) / 100.0);
            semesterData.put("credits", totalCredits);
            semesterData.put("courseCount", courses.size());

            semesterGPAs.add(semesterData);

            System.out.println("✓ Semester " + semester + ": GPA = " + String.format("%.2f", gpa) +
                    ", Credits = " + totalCredits + ", Courses = " + courses.size());
        }

        return semesterGPAs;
    }

    private double calculateOverallCGPA(List<Map<String, Object>> semesterGPAs) {
        double totalWeightedGPA = 0;
        int totalCredits = 0;

        for (Map<String, Object> sem : semesterGPAs) {
            double gpa = (Double) sem.get("gpa");
            int credits = (Integer) sem.get("credits");

            totalWeightedGPA += (gpa * credits);
            totalCredits += credits;
        }

        double cgpa = totalCredits > 0 ? totalWeightedGPA / totalCredits : 0.0;
        System.out.println("✓ Overall CGPA: " + String.format("%.2f", cgpa) + " (Total credits: " + totalCredits + ")");

        return Math.round(cgpa * 100.0) / 100.0;
    }

    private double convertGradeToPoint(String grade) {
        if (grade == null || grade.isEmpty() || grade.equals("-")) {
            return 0.0;
        }

        // Extract letter grade (remove numbers like "6 B" -> "B")
        String letterGrade = grade.replaceAll("[0-9]", "").trim();

        switch (letterGrade.toUpperCase()) {
            case "O": return 10.0;
            case "A+": return 9.0;
            case "A": return 8.0;
            case "B+": return 7.0;
            case "B": return 6.0;
            case "C": return 5.0;
            case "D": return 4.0;
            default: return 0.0;
        }
    }

    @Override
    public void close() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error closing driver: " + e.getMessage());
            }
        }
    }
}