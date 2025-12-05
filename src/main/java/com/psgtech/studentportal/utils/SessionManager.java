package com.psgtech.studentportal.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Session Manager with proper ASP.NET handling
 */
public class SessionManager {

    private static SessionManager instance;
    private Map<String, String> studzone1Cookies;
    private Map<String, String> studzone2Cookies;
    private String rollNo;
    private String loggedInStudentRollNo;
    private String studentName;
    private boolean isLoggedIn;
    private boolean studzone2Initialized = false;

    private static final String STUDZONE1_URL = "https://ecampus.psgtech.ac.in/studzone";
    private static final String STUDZONE2_URL = "https://ecampus.psgtech.ac.in/studzone2/";
    private static final int TIMEOUT = 30000;

    public SessionManager() {
        studzone1Cookies = new HashMap<>();
        studzone2Cookies = new HashMap<>();
        isLoggedIn = false;
        loggedInStudentRollNo = null;
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setLoggedInStudent(String rollNo) {
        this.loggedInStudentRollNo = rollNo;
        System.out.println("✅ Student logged in: " + rollNo);
    }

    public String getLoggedInStudentRollNo() {
        return loggedInStudentRollNo;
    }

    public boolean isUserLoggedIn() {
        return loggedInStudentRollNo != null;
    }

    public void logout() {
        loggedInStudentRollNo = null;
        clearSession();
        System.out.println("✅ User logged out");
    }

    public boolean loginToStudzone1(String rollNo, String password) throws IOException {
        System.out.println("🔐 Logging into Studzone1...");

        try {
            Connection.Response loginPageResponse = Jsoup.connect(STUDZONE1_URL)
                    .method(Connection.Method.GET)
                    .timeout(TIMEOUT)
                    .execute();

            Document loginPage = loginPageResponse.parse();
            String token = loginPage.select("input[name=__RequestVerificationToken]").val();

            if (token == null || token.isEmpty()) {
                System.err.println("❌ Could not extract verification token from login page");
                return false;
            }

            studzone1Cookies.putAll(loginPageResponse.cookies());

            Map<String, String> loginData = new HashMap<>();
            loginData.put("rollno", rollNo);
            loginData.put("password", password);
            loginData.put("chkterms", "on");
            loginData.put("__RequestVerificationToken", token);

            Connection.Response loginResponse = Jsoup.connect(STUDZONE1_URL)
                    .data(loginData)
                    .cookies(studzone1Cookies)
                    .method(Connection.Method.POST)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            studzone1Cookies.putAll(loginResponse.cookies());

            Document responsePage = loginResponse.parse();
            boolean success = responsePage.select("nav.navbar.navbar-expand-lg.navbar-light").size() > 0;

            if (success) {
                this.rollNo = rollNo;
                this.isLoggedIn = true;
                System.out.println("✅ Studzone1 login successful!");
            } else {
                System.err.println("❌ Studzone1 login failed");
            }

            return success;

        } catch (IOException e) {
            System.err.println("❌ Network error during Studzone1 login: " + e.getMessage());
            throw e;
        }
    }

    public boolean loginToStudzone2(String rollNo, String password) throws IOException {
        System.out.println("🔐 Logging into Studzone2...");

        try {
            Thread.sleep(300); // Reduced from 1000ms

            Connection.Response loginPageResponse = Jsoup.connect(STUDZONE2_URL)
                    .method(Connection.Method.GET)
                    .timeout(TIMEOUT)
                    .execute();

            Document loginPage = loginPageResponse.parse();

            String viewState = loginPage.select("input[name=__VIEWSTATE]").val();
            String viewStateGenerator = loginPage.select("input[name=__VIEWSTATEGENERATOR]").val();
            String eventValidation = loginPage.select("input[name=__EVENTVALIDATION]").val();
            String abcd3 = loginPage.select("input[name=abcd3]").val();

            if (viewState == null || viewState.isEmpty()) {
                System.err.println("❌ Could not extract ViewState tokens");
                return false;
            }

            System.out.println("✅ Extracted ViewState tokens");

            studzone2Cookies.putAll(loginPageResponse.cookies());

            Map<String, String> loginData = new HashMap<>();
            loginData.put("__EVENTTARGET", "");
            loginData.put("__EVENTARGUMENT", "");
            loginData.put("__LASTFOCUS", "");
            loginData.put("__VIEWSTATE", viewState);
            loginData.put("__VIEWSTATEGENERATOR", viewStateGenerator);
            loginData.put("__EVENTVALIDATION", eventValidation);
            loginData.put("rdolst", "S");
            loginData.put("txtusercheck", rollNo);
            loginData.put("txtpwdcheck", password);
            loginData.put("abcd3", abcd3);

            Connection.Response loginResponse = Jsoup.connect(STUDZONE2_URL)
                    .data(loginData)
                    .cookies(studzone2Cookies)
                    .method(Connection.Method.POST)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            studzone2Cookies.putAll(loginResponse.cookies());

            Thread.sleep(500); // Reduced from 1500ms

            // Initialize by visiting main page
            initializeStudzone2Session();

            System.out.println("✅ Studzone2 login successful!");
            return true;

        } catch (InterruptedException e) {
            System.err.println("❌ Thread interrupted during login");
            throw new IOException("Interrupted", e);
        } catch (IOException e) {
            System.err.println("❌ Network error during Studzone2 login: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Initialize Studzone2 session by visiting main page
     */
    private void initializeStudzone2Session() throws IOException {
        if (studzone2Initialized)
            return;

        try {
            System.out.println("🔄 Initializing Studzone2 session...");

            // Try common landing pages in order
            String[] possiblePages = {
                    STUDZONE2_URL + "HomePage.aspx",
                    STUDZONE2_URL + "FrmEpsStudCourse.aspx",
                    STUDZONE2_URL + "FrmEpsStudHome.aspx",
                    STUDZONE2_URL + "AttWfStudCourseSelection.aspx"
            };

            boolean initialized = false;
            for (String pageUrl : possiblePages) {
                try {
                    Connection.Response response = Jsoup.connect(pageUrl)
                            .cookies(studzone2Cookies)
                            .method(Connection.Method.GET)
                            .timeout(TIMEOUT)
                            .followRedirects(true)
                            .ignoreHttpErrors(true)
                            .execute();

                    if (response.statusCode() == 200) {
                        studzone2Cookies.putAll(response.cookies());
                        studzone2Initialized = true;
                        initialized = true;
                        System.out.println("✅ Studzone2 session initialized via: " + pageUrl);
                        break;
                    }
                } catch (IOException e) {
                    // Try next page
                    continue;
                }
            }

            if (!initialized) {
                System.out.println("⚠️ Could not find valid landing page, proceeding anyway...");
                studzone2Initialized = true; // Proceed anyway
            }

            Thread.sleep(200); // Reduced from 1000ms

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Document fetchStudzone1Page(String url) throws IOException {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in to Studzone1");
        }

        try {
            Connection.Response response = Jsoup.connect(url)
                    .cookies(studzone1Cookies)
                    .method(Connection.Method.GET)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            studzone1Cookies.putAll(response.cookies());
            return response.parse();

        } catch (IOException e) {
            System.err.println("❌ Error fetching Studzone1 page: " + url);
            throw e;
        }
    }

    /**
     * Enhanced Studzone2 page fetcher with retry logic
     */
    public Document fetchStudzone2Page(String url) throws IOException {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                // Ensure session is initialized
                if (!studzone2Initialized) {
                    initializeStudzone2Session();
                }

                Connection.Response response = Jsoup.connect(url)
                        .cookies(studzone2Cookies)
                        .method(Connection.Method.GET)
                        .timeout(TIMEOUT)
                        .followRedirects(true)
                        .ignoreHttpErrors(true) // Don't throw on 500
                        .execute();

                int statusCode = response.statusCode();

                if (statusCode == 200) {
                    studzone2Cookies.putAll(response.cookies());
                    return response.parse();
                } else if (statusCode == 500 && retryCount < maxRetries - 1) {
                    System.out.println(
                            "⚠️ Got 500 error, retrying after session refresh... (attempt " + (retryCount + 1) + ")");
                    studzone2Initialized = false;
                    Thread.sleep(1000); // Reduced from 2000ms
                    retryCount++;
                    continue;
                } else {
                    throw new IOException("HTTP error fetching URL. Status=" + statusCode + ", URL=[" + url + "]");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while retrying", e);
            } catch (IOException e) {
                if (retryCount < maxRetries - 1) {
                    System.out.println("⚠️ Fetch failed, retrying... (attempt " + (retryCount + 1) + ")");
                    retryCount++;
                    try {
                        Thread.sleep(1000); // Reduced from 2000ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("❌ Error fetching Studzone2 page: " + url);
                    throw e;
                }
            }
        }

        throw new IOException("Max retries exceeded for: " + url);
    }

    public Document postStudzone1Page(String url, Map<String, String> data) throws IOException {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in to Studzone1");
        }

        try {
            Connection.Response response = Jsoup.connect(url)
                    .data(data)
                    .cookies(studzone1Cookies)
                    .method(Connection.Method.POST)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            studzone1Cookies.putAll(response.cookies());
            return response.parse();

        } catch (IOException e) {
            System.err.println("❌ Error posting to Studzone1 page: " + url);
            throw e;
        }
    }

    public Document postStudzone2Page(String url, Map<String, String> data) throws IOException {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .data(data)
                    .cookies(studzone2Cookies)
                    .method(Connection.Method.POST)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            studzone2Cookies.putAll(response.cookies());
            return response.parse();

        } catch (IOException e) {
            System.err.println("❌ Error posting to Studzone2 page: " + url);
            throw e;
        }
    }

    public String fetchStudentName() throws IOException {
        if (studentName != null && !studentName.isEmpty()) {
            return studentName;
        }

        try {
            String profileUrl = "https://ecampus.psgtech.ac.in/studzone/Home/Profile";
            Document profilePage = fetchStudzone1Page(profileUrl);
            studentName = profilePage.select("h2.profile-name").text().trim();

            if (studentName.isEmpty()) {
                studentName = "Student";
            }

            System.out.println("👤 Student Name: " + studentName);
            return studentName;

        } catch (IOException e) {
            System.err.println("❌ Could not fetch student name");
            studentName = "Student";
            return studentName;
        }
    }

    public String getGreeting() {
        try {
            String name = fetchStudentName();
            return "Welcome, " + name + "!";
        } catch (IOException e) {
            return "Welcome, Student!";
        }
    }

    public boolean isSessionValid() {
        try {
            String testUrl = "https://ecampus.psgtech.ac.in/studzone/Home/Profile";
            Document testPage = fetchStudzone1Page(testUrl);
            return testPage.select("h2.profile-name").size() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean refreshSession(String rollNo, String password) {
        System.out.println("🔄 Refreshing expired session...");
        clearSession();
        try {
            return loginToStudzone1(rollNo, password) && loginToStudzone2(rollNo, password);
        } catch (IOException e) {
            System.err.println("❌ Failed to refresh session");
            return false;
        }
    }

    public void clearSession() {
        studzone1Cookies.clear();
        studzone2Cookies.clear();
        rollNo = null;
        studentName = null;
        isLoggedIn = false;
        studzone2Initialized = false;
        System.out.println("🔓 Session cleared - logged out");
    }

    // Getters
    public String getRollNo() {
        return rollNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public Map<String, String> getStudzone1Cookies() {
        return new HashMap<>(studzone1Cookies);
    }

    public Map<String, String> getStudzone2Cookies() {
        return new HashMap<>(studzone2Cookies);
    }
}