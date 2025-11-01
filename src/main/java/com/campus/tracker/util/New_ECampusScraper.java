package com.campus.tracker.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.NoSuchElementException;
import java.time.Duration;
import java.util.*;

public class New_ECampusScraper implements AutoCloseable {
    private WebDriver driver;
    private WebDriverWait wait;

    public New_ECampusScraper() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        options.addArguments("--no-sandbox", "--disable-gpu", "--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public Map<String, Object> loginAndScrape(String username, String password) {
        try {
            System.out.println("Opening new eCampus login...");
            driver.get("https://ecampus.psgtech.ac.in/studzone/Login/StudLogin");
            Thread.sleep(2000);

            System.out.println("Current URL: " + driver.getCurrentUrl());

            // === FIND AND FILL USERNAME ===
            List<WebElement> textInputs = driver.findElements(By.cssSelector("input[type='text']"));
            WebElement rollNoField = null;

            for (WebElement input : textInputs) {
                String placeholder = input.getAttribute("placeholder");
                String name = input.getAttribute("name");
                String id = input.getAttribute("id");

                if ((placeholder != null && placeholder.toLowerCase().contains("roll")) ||
                        (name != null && name.toLowerCase().contains("roll")) ||
                        (id != null && id.toLowerCase().contains("roll")) ||
                        (name != null && name.toLowerCase().contains("user"))) {
                    rollNoField = input;
                    break;
                }
            }

            if (rollNoField == null && !textInputs.isEmpty()) {
                rollNoField = textInputs.get(0);
            }

            if (rollNoField != null) {
                rollNoField.clear();
                rollNoField.sendKeys(username);
                System.out.println("✓ Roll No entered");
            } else {
                throw new RuntimeException("Could not find username field");
            }

            // === FIND AND FILL PASSWORD ===
            WebElement passwordField = driver.findElement(By.cssSelector("input[type='password']"));
            passwordField.clear();
            passwordField.sendKeys(password);
            System.out.println("✓ Password entered");

            // === CHECK ALL CHECKBOXES (CRITICAL!) ===
            List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
            System.out.println("Found " + checkboxes.size() + " checkbox(es)");

            for (int i = 0; i < checkboxes.size(); i++) {
                try {
                    WebElement checkbox = checkboxes.get(i);
                    if (!checkbox.isSelected()) {
                        try {
                            checkbox.click();
                        } catch (Exception e1) {
                            try {
                                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);
                            } catch (Exception e2) {
                                ((JavascriptExecutor) driver).executeScript("arguments[0].checked = true;", checkbox);
                            }
                        }
                        Thread.sleep(300);
                        System.out.println("✓ Checkbox " + (i + 1) + " checked");
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not check checkbox " + (i + 1));
                }
            }

            // === FIND AND CLICK LOGIN BUTTON ===
            WebElement loginBtn = null;

            try {
                loginBtn = driver.findElement(By.xpath("//button[@type='submit']"));
            } catch (Exception e1) {
                try {
                    loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
                } catch (Exception e2) {
                    try {
                        loginBtn = driver.findElement(By.xpath("//button[contains(text(), 'Login') or contains(text(), 'login')]"));
                    } catch (Exception e3) {
                        List<WebElement> buttons = driver.findElements(By.tagName("button"));
                        if (!buttons.isEmpty()) {
                            loginBtn = buttons.get(0);
                        }
                    }
                }
            }

            if (loginBtn != null) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", loginBtn);
                Thread.sleep(500);

                try {
                    loginBtn.click();
                } catch (Exception e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);
                }
                System.out.println("✓ Login button clicked");
            } else {
                throw new RuntimeException("Could not find login button");
            }

            // === WAIT FOR REDIRECT ===
            Thread.sleep(3000);

            for (int i = 0; i < 10; i++) {
                String currentUrl = driver.getCurrentUrl();
                if (!currentUrl.contains("Login") && !currentUrl.contains("login")) {
                    break;
                }
                Thread.sleep(500);
            }

            String currentUrl = driver.getCurrentUrl();
            System.out.println("After login URL: " + currentUrl);

            if (currentUrl.contains("Login") || currentUrl.contains("login")) {
                try {
                    List<WebElement> errorElements = driver.findElements(By.cssSelector(".alert-danger, .error, .text-danger, .invalid-feedback"));
                    for (WebElement errorEl : errorElements) {
                        if (errorEl.isDisplayed()) {
                            throw new RuntimeException("Login failed: " + errorEl.getText());
                        }
                    }
                } catch (NoSuchElementException e) {
                    // No error message found
                }
                throw new RuntimeException("Login failed - still on login page");
            }

            System.out.println("✓ New eCampus login successful!");

            // === SCRAPE CA MARKS ===
            Map<String, Object> data = new HashMap<>();
            data.put("caMarks", scrapeCAMarks());
            return data;

        } catch (Exception e) {
            System.err.println("New eCampus error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("New eCampus login failed: " + e.getMessage(), e);
        }
    }

    private List<Map<String, String>> scrapeCAMarks() {
        List<Map<String, String>> caMarks = new ArrayList<>();
        try {
            System.out.println("Navigating to CA Marks page...");

            ((JavascriptExecutor) driver).executeScript(
                    "window.location.href = 'https://ecampus.psgtech.ac.in/studzone/ContinuousAssessment/CAMarksView';"
            );
            Thread.sleep(4000);

            String currentUrl = driver.getCurrentUrl();
            System.out.println("CA Marks page URL: " + currentUrl);

            if (currentUrl.contains("Login") || currentUrl.contains("login")) {
                System.err.println("✗ Session expired - redirected to login");
                return caMarks;
            }

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
            } catch (Exception e) {
                System.err.println("✗ No tables found on CA Marks page");
                return caMarks;
            }

            List<WebElement> tables = driver.findElements(By.tagName("table"));
            System.out.println("Found " + tables.size() + " table(s)");

            for (int tableIdx = 0; tableIdx < tables.size(); tableIdx++) {
                WebElement table = tables.get(tableIdx);
                List<WebElement> rows = table.findElements(By.tagName("tr"));
                System.out.println("\nProcessing Table " + (tableIdx + 1) + " with " + rows.size() + " rows");

                if (rows.isEmpty()) continue;

                List<WebElement> headerCells = rows.get(0).findElements(By.tagName("th"));
                boolean isLabTable = false;
                boolean isTheoryTable = false;

                for (WebElement header : headerCells) {
                    String headerText = header.getText().trim().toUpperCase();
                    if (headerText.contains("LT1") || headerText.contains("LT2")) {
                        isLabTable = true;
                        System.out.println("→ Lab subjects table detected");
                        break;
                    } else if (headerText.contains("T1") && headerText.contains("T2") && headerText.contains("RT")) {
                        isTheoryTable = true;
                        System.out.println("→ Theory subjects table detected");
                        break;
                    }
                }

                for (int i = 1; i < rows.size(); i++) {
                    try {
                        List<WebElement> cells = rows.get(i).findElements(By.tagName("td"));
                        if (cells.size() < 3) continue;

                        String code = cells.get(0).getText().trim();
                        String title = cells.get(1).getText().trim();

                        if (code.isEmpty() ||
                                code.equalsIgnoreCase("COURSE CODE") ||
                                code.matches("^\\d+$")) {
                            continue;
                        }

                        Map<String, String> entry = new HashMap<>();
                        entry.put("courseCode", code);
                        entry.put("courseTitle", title);

                        if (isLabTable) {
                            if (cells.size() >= 6) {
                                entry.put("lt1", cells.get(2).getText().trim());
                                entry.put("lt2", cells.get(3).getText().trim());
                                entry.put("total", cells.get(4).getText().trim());
                                entry.put("convTotal", cells.get(5).getText().trim());
                                entry.put("type", "LAB");
                            }
                        } else if (isTheoryTable) {
                            if (cells.size() >= 13) {
                                entry.put("t1", cells.get(2).getText().trim());
                                entry.put("t2", cells.get(3).getText().trim());
                                entry.put("rt", cells.get(4).getText().trim());
                                entry.put("rt1", cells.get(5).getText().trim());
                                entry.put("rt2", cells.get(6).getText().trim());
                                entry.put("totalBeforeAP", cells.get(7).getText().trim());
                                entry.put("ap", cells.get(8).getText().trim());
                                entry.put("mp1", cells.get(9).getText().trim());
                                entry.put("mp2", cells.get(10).getText().trim());
                                entry.put("total", cells.get(11).getText().trim());
                                entry.put("convTotal", cells.get(12).getText().trim());
                                entry.put("type", "THEORY");
                            }
                        }

                        caMarks.add(entry);
                        System.out.println("  ✓ " + code + " - " + title + " [" + entry.getOrDefault("type", "UNKNOWN") + "]");

                    } catch (Exception e) {
                        continue;
                    }
                }
            }

            System.out.println("\n✓ Total CA marks entries: " + caMarks.size());

        } catch (Exception e) {
            System.err.println("CA marks scraping error: " + e.getMessage());
            e.printStackTrace();
        }
        return caMarks;
    }

    @Override
    public void close() throws Exception {  // ← THIS IS THE FIX!
        if (driver != null) {
            driver.quit();
        }
    }
}