package com.campus.tracker.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.*;

public class New_ECampusScraper implements AutoCloseable {
    private WebDriver driver;
    private WebDriverWait wait;

    public New_ECampusScraper() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        options.addArguments("--no-sandbox", "--disable-gpu", "--window-size=1920,1080", "--headless");  // NEW: Headless for stability
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        options.addArguments("--disable-web-security", "--allow-running-insecure-content");  // NEW: For flaky sites
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);  // NEW: Faster load
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));  // Increased timeout
    }

    public Map<String, Object> loginAndScrape(String rollNo, String password) {
        Map<String, Object> data = new HashMap<>();
        try {
            // NEW: Retry get with sleep
            int retries = 3;
            for (int i = 0; i < retries; i++) {
                try {
                    System.out.println("Opening new eCampus login (attempt " + (i+1) + ")...");
                    Thread.sleep(2000 * i);  // Progressive delay
                    driver.get("https://ecampus.psgtech.ac.in/studzone/Login/StudLogin");
                    break;
                } catch (TimeoutException e) {
                    if (i == retries - 1) throw e;
                    System.out.println("  Retry due to timeout...");
                }
            }

            // Login (unchanged, but with retry on elements)
            WebElement rollField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("RollNo")));
            rollField.clear();
            rollField.sendKeys(rollNo);
            System.out.println("✓ Roll No entered");

            WebElement passField = driver.findElement(By.id("Password"));
            passField.clear();
            passField.sendKeys(password);
            System.out.println("✓ Password entered");

            List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
            System.out.println("Found " + checkboxes.size() + " checkbox(es)");
            if (!checkboxes.isEmpty()) {
                checkboxes.get(0).click();
                System.out.println("✓ Checkbox checked");
            }

            WebElement loginBtn = driver.findElement(By.id("loginButton"));
            loginBtn.click();
            System.out.println("✓ Login button clicked");

            wait.until(ExpectedConditions.urlContains("Home/Menu"));
            System.out.println("After login URL: " + driver.getCurrentUrl());
            System.out.println("✓ New eCampus login successful!");

            // Scrape CA marks (unchanged)
            data.put("caMarks", scrapeCAMarks());

            return data;

        } catch (Exception e) {
            System.err.println("New eCampus error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("New eCampus login failed", e);
        }
    }

    private List<Map<String, Object>> scrapeCAMarks() {
        List<Map<String, Object>> caMarks = new ArrayList<>();
        try {
            System.out.println("Navigating to CA Marks page...");
            driver.get("https://ecampus.psgtech.ac.in/studzone/ContinuousAssessment/CAMarksView");
            Thread.sleep(5000);

            System.out.println("CA Marks page URL: " + driver.getCurrentUrl());

            List<WebElement> tables = driver.findElements(By.tagName("table"));
            System.out.println("Found " + tables.size() + " table(s)");

            for (int t = 0; t < tables.size(); t++) {
                WebElement table = tables.get(t);
                List<WebElement> rows = table.findElements(By.tagName("tr"));
                System.out.println("Processing Table " + (t+1) + " with " + rows.size() + " rows");

                for (int i = 1; i < rows.size(); i++) {  // Skip header
                    List<WebElement> cells = rows.get(i).findElements(By.tagName("td"));
                    if (cells.size() < 5) continue;

                    String courseCode = cells.get(0).getText().trim();
                    if (!courseCode.matches("23XT\\d{2}.*")) continue;  // Validate

                    String courseTitle = cells.get(1).getText().trim();
                    double t1 = parseDoubleOrZero(cells.get(2).getText().trim());
                    double t2 = parseDoubleOrZero(cells.get(3).getText().trim());
                    double caTotal = t1 + t2;

                    Map<String, Object> row = new HashMap<>();
                    row.put("courseCode", courseCode);
                    row.put("courseTitle", courseTitle);
                    row.put("t1", t1);
                    row.put("t2", t2);
                    row.put("caTotal", caTotal);
                    caMarks.add(row);

                    System.out.println("  ✓ " + courseCode + " - " + courseTitle + " (T1: " + t1 + ", T2: " + t2 + ")");
                }
            }

            System.out.println("✓ Total CA marks entries: " + caMarks.size());

        } catch (Exception e) {
            System.err.println("Error scraping CA marks: " + e.getMessage());
        }
        return caMarks;
    }

    private double parseDoubleOrZero(String text) {
        try {
            return Double.parseDouble(text.replace("%", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public void close() {
        if (driver != null) {
            try {
                System.out.println("Closing new eCampus driver...");
                driver.quit();
            } catch (Exception e) {}
        }
    }
}