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
    private Set<String> seenCourses = new HashSet<>();  // NEW: Prevent duplicates

    public ECampusScraper() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        options.addArguments("--no-sandbox", "--disable-gpu", "--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public Map<String, Object> loginAndScrape(String username, String password) {
        Map<String, Object> data = new HashMap<>();
        seenCourses.clear();  // Reset per login
        try {
            System.out.println("Opening old eCampus...");
            driver.get("https://ecampus.psgtech.ac.in/studzone2/");
            Thread.sleep(3000);

            // Login (unchanged)
            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("txtusercheck")));
            userField.clear();
            userField.sendKeys(username);
            System.out.println("✓ Username entered");

            WebElement passField = driver.findElement(By.id("txtpwdcheck"));
            passField.clear();
            passField.sendKeys(password);
            System.out.println("✓ Password entered");

            WebElement loginBtn = driver.findElement(By.id("abcd3"));
            loginBtn.click();
            System.out.println("✓ Login button clicked");

            try {
                Thread.sleep(1000);
                Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                alert.accept();
                throw new RuntimeException("Login failed: " + alertText);
            } catch (NoAlertPresentException e) {
                System.out.println("✓ No alert, login successful");
            }

            wait.until(ExpectedConditions.urlContains("AttWfStudMenu.aspx"));
            Thread.sleep(3000);
            System.out.println("✓ Login successful!");

            // Student info (unchanged)
            try {
                String name = driver.findElement(By.id("Title1_LblStaffName")).getText().trim();
                data.put("name", name);
                data.put("rollNo", username);
                data.put("programme", "MSc Theoretical Computer Science");
            } catch (Exception e) {
                data.put("name", "Student");
                data.put("rollNo", username);
                data.put("programme", "Unknown");
            }

            data.put("examResults", scrapeFromCourseSelectionPage());
            return data;

        } catch (Exception e) {
            System.err.println("Old eCampus error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Old eCampus login failed", e);
        }
    }

    private List<Map<String, String>> scrapeFromCourseSelectionPage() {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            System.out.println("Navigating to Course Selection page...");
            driver.get("https://ecampus.psgtech.ac.in/studzone2/AttWfStudCourseSelection.aspx");
            Thread.sleep(5000);

            System.out.println("Current URL: " + driver.getCurrentUrl());

            List<WebElement> tables = driver.findElements(By.tagName("table"));
            System.out.println("Found " + tables.size() + " tables on page");

            for (WebElement table : tables) {
                List<WebElement> rows = table.findElements(By.tagName("tr"));
                if (rows.size() < 2) continue;  // Skip empty tables

                String tableText = table.getText();
                boolean isCompleted = tableText.contains("Details of Courses Completed") ||
                        (tableText.contains("COURSE CODE") && tableText.contains("GRADE"));
                boolean isCurrent = tableText.contains("Details of Courses Currently Studying") ||
                        (tableText.contains("COURSE CODE") && tableText.contains("THEORY OF COMPUTING"));

                if (!isCompleted && !isCurrent) continue;

                System.out.println("\n✓ Found " + (isCompleted ? "'Courses Completed'" : "'Currently Studying'") + " table");

                // Start from row 1 (skip header)
                for (int i = 1; i < rows.size(); i++) {
                    try {
                        List<WebElement> cells = rows.get(i).findElements(By.tagName("td"));
                        if (cells.size() < 6) continue;

                        String courseCode = cells.get(1).getText().trim();
                        if (courseCode.isEmpty() || !courseCode.matches("23XT\\d{2}.*")) continue;  // NEW: Validate code pattern
                        if (seenCourses.contains(courseCode)) continue;  // NEW: Skip duplicates
                        seenCourses.add(courseCode);

                        String courseTitle = cells.get(2).getText().trim();
                        String courseSem = cells.get(4).getText().trim();  // COURSE SEM column
                        String credits = cells.get(7).getText().trim();    // CREDITS (index 7 for completed)
                        String grade = isCompleted ? cells.get(6).getText().trim() : "In Progress";

                        if (grade.isEmpty() || grade.equals("-")) continue;

                        String semester = extractSemesterFromCode(courseCode, courseSem);
                        if (isCurrent) semester = "5";  // Force Sem 5 for current

                        Map<String, String> course = new HashMap<>();
                        course.put("courseCode", courseCode);
                        course.put("courseTitle", courseTitle);
                        course.put("credits", credits);
                        course.put("grade", grade);
                        course.put("semester", semester);
                        course.put("result", isCompleted ? "Pass" : "In Progress");

                        results.add(course);
                        System.out.println("  ✓ Sem " + semester + ": " + courseCode + " - " + courseTitle + " (" + grade + ")");

                    } catch (Exception e) {
                        continue;  // Skip bad rows
                    }
                }
            }

            System.out.println("\n✓ Total unique courses scraped: " + results.size());

        } catch (Exception e) {
            System.err.println("Error scraping course selection: " + e.getMessage());
        }
        return results;
    }

    private String extractSemesterFromCode(String courseCode, String courseSem) {
        // Extract from code: 23XT41 → '4'
        if (courseCode.length() >= 6) {
            char semChar = courseCode.charAt(4);
            if (Character.isDigit(semChar)) return String.valueOf(semChar);
        }
        // Fallback to courseSem
        return courseSem.isEmpty() ? "Unknown" : courseSem;
    }

    @Override
    public void close() {
        if (driver != null) {
            try {
                System.out.println("Closing old eCampus driver...");
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error closing driver: " + e.getMessage());
            }
        }
    }
}