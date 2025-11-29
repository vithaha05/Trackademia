package com.psgtech.studentportal.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Session Manager for handling HTTP requests to PSG Tech eCampus
 * Maintains sessions across studzone and studzone2
 * Also manages local login session
 */
public class SessionManager {

    private static SessionManager instance;
    private Map<String, String> studzone1Cookies;
    private Map<String, String> studzone2Cookies;
    private String rollNo;
    private String loggedInStudentRollNo;
    private String studentName;
    private boolean isLoggedIn;

    private static final String STUDZONE1_URL = "https://ecampus.psgtech.ac.in/studzone";
    private static final String STUDZONE2_URL = "https://ecampus.psgtech.ac.in/studzone2/";
    private static final int TIMEOUT = 30000; // 30 seconds timeout

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

    /**
     * Set the logged-in student roll number (for local app login)
     */
    public void setLoggedInStudent(String rollNo) {
        this.loggedInStudentRollNo = rollNo;
        System.out.println("✅ Student logged in: " + rollNo);
    }

    /**
     * Get the logged-in student roll number
     */
    public String getLoggedInStudentRollNo() {
        return loggedInStudentRollNo;
    }

    /**
     * Check if user is logged in to the app
     */
    public boolean isUserLoggedIn() {
        return loggedInStudentRollNo != null;
    }

    /**
     * Logout the user from the app
     */
    public void logout() {
        loggedInStudentRollNo = null;
        clearSession();
        System.out.println("✅ User logged out");
    }

    /**
     * Login to Studzone1 (Main Portal - Attendance, Internals, etc.)
     */
    public boolean loginToStudzone1(String rollNo, String password) throws IOException {
        System.out.println("🔐 Logging into Studzone1...");

        try {
            // Step 1: Get login page to extract CSRF token
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

            // Store cookies from login page
            studzone1Cookies.putAll(loginPageResponse.cookies());

            // Step 2: Prepare login payload
            Map<String, String> loginData = new HashMap<>();
            loginData.put("rollno", rollNo);
            loginData.put("password", password);
            loginData.put("chkterms", "on");
            loginData.put("__RequestVerificationToken", token);

            // Step 3: Post login credentials
            Connection.Response loginResponse = Jsoup.connect(STUDZONE1_URL)
                    .data(loginData)
                    .cookies(studzone1Cookies)
                    .method(Connection.Method.POST)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            // Update cookies after login
            studzone1Cookies.putAll(loginResponse.cookies());

            // Step 4: Verify login success by checking for navbar (present only after login)
            Document responsePage = loginResponse.parse();
            boolean success = responsePage.select("nav.navbar.navbar-expand-lg.navbar-light").size() > 0;

            if (success) {
                this.rollNo = rollNo;
                this.isLoggedIn = true;
                System.out.println("✅ Studzone1 login successful!");
            } else {
                System.err.println("❌ Studzone1 login failed - invalid credentials or page structure changed");
            }

            return success;

        } catch (IOException e) {
            System.err.println("❌ Network error during Studzone1 login: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Login to Studzone2 (CGPA Portal - Courses, Grades, Results)
     */
    public boolean loginToStudzone2(String rollNo, String password) throws IOException {
        System.out.println("🔐 Logging into Studzone2...");

        try {
            // Step 1: Get login page to extract dynamic tokens
            Connection.Response loginPageResponse = Jsoup.connect(STUDZONE2_URL)
                    .method(Connection.Method.GET)
                    .timeout(TIMEOUT)
                    .execute();

            Document loginPage = loginPageResponse.parse();

            // Extract ASP.NET ViewState tokens
            String viewState = loginPage.select("input[name=__VIEWSTATE]").val();
            String viewStateGenerator = loginPage.select("input[name=__VIEWSTATEGENERATOR]").val();
            String eventValidation = loginPage.select("input[name=__EVENTVALIDATION]").val();
            String abcd3 = loginPage.select("input[name=abcd3]").val();

            if (viewState == null || viewState.isEmpty()) {
                System.err.println("❌ Could not extract ViewState tokens from Studzone2 login page");
                return false;
            }

            // Store cookies
            studzone2Cookies.putAll(loginPageResponse.cookies());

            // Step 2: Prepare login data with all required ASP.NET tokens
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

            // Step 3: Post login
            Connection.Response loginResponse = Jsoup.connect(STUDZONE2_URL)
                    .data(loginData)
                    .cookies(studzone2Cookies)
                    .method(Connection.Method.POST)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            // Update cookies after login
            studzone2Cookies.putAll(loginResponse.cookies());

            System.out.println("✅ Studzone2 login successful!");
            return true;

        } catch (IOException e) {
            System.err.println("❌ Network error during Studzone2 login: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fetch page from Studzone1 using established session
     */
    public Document fetchStudzone1Page(String url) throws IOException {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in to Studzone1. Please login first.");
        }

        try {
            Connection.Response response = Jsoup.connect(url)
                    .cookies(studzone1Cookies)
                    .method(Connection.Method.GET)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            // Update cookies in case they changed
            studzone1Cookies.putAll(response.cookies());

            return response.parse();

        } catch (IOException e) {
            System.err.println("❌ Error fetching Studzone1 page: " + url);
            throw e;
        }
    }

    /**
     * Fetch page from Studzone2 using established session
     */
    public Document fetchStudzone2Page(String url) throws IOException {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .cookies(studzone2Cookies)
                    .method(Connection.Method.GET)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .execute();

            // Update cookies
            studzone2Cookies.putAll(response.cookies());

            return response.parse();

        } catch (IOException e) {
            System.err.println("❌ Error fetching Studzone2 page: " + url);
            throw e;
        }
    }

    /**
     * Post data to Studzone1
     */
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

    /**
     * Post data to Studzone2
     */
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

    /**
     * Get student name from profile page
     */
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
            System.err.println("❌ Could not fetch student name, using default");
            studentName = "Student";
            return studentName;
        }
    }

    /**
     * Get personalized greeting with birthday check
     */
    public String getGreeting() {
        try {
            String name = fetchStudentName();
            return "Welcome, " + name + "!";
        } catch (IOException e) {
            return "Welcome, Student!";
        }
    }

    /**
     * Check if session is still valid by testing a request
     */
    public boolean isSessionValid() {
        try {
            String testUrl = "https://ecampus.psgtech.ac.in/studzone/Home/Profile";
            Document testPage = fetchStudzone1Page(testUrl);
            return testPage.select("h2.profile-name").size() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Refresh session if expired
     */
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

    /**
     * Clear all session data (logout)
     */
    public void clearSession() {
        studzone1Cookies.clear();
        studzone2Cookies.clear();
        rollNo = null;
        studentName = null;
        isLoggedIn = false;
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

    /**
     * Main method for testing session management
     */
    public static void main(String[] args) {
        System.out.println("🧪 Testing Session Manager...\n");

        SessionManager sessionManager = SessionManager.getInstance();

        try {
            System.out.println("Testing Studzone1 login...");
            boolean studzone1Success = sessionManager.loginToStudzone1("test123", "password");
            System.out.println("Studzone1 Result: " + (studzone1Success ? "✅ Success" : "❌ Failed"));

            if (studzone1Success) {
                System.out.println("\nTesting Studzone2 login...");
                boolean studzone2Success = sessionManager.loginToStudzone2("test123", "password");
                System.out.println("Studzone2 Result: " + (studzone2Success ? "✅ Success" : "❌ Failed"));

                System.out.println("\nFetching student name...");
                String name = sessionManager.fetchStudentName();
                System.out.println("Student Name: " + name);

                System.out.println("\nChecking session validity...");
                boolean isValid = sessionManager.isSessionValid();
                System.out.println("Session Valid: " + (isValid ? "✅ Yes" : "❌ No"));
            }

        } catch (IOException e) {
            System.err.println("❌ Test failed with exception:");
            e.printStackTrace();
        } finally {
            sessionManager.clearSession();
            System.out.println("\n✅ Test completed");
        }
    }
}