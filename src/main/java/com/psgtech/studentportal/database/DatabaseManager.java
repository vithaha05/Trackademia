package com.psgtech.studentportal.database;

import java.sql.*;
import java.util.Properties;

/**
 * Database Manager for MySQL
 * Handles all database operations and connections
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    // MySQL Connection Details - UPDATE THESE WITH YOUR CREDENTIALS
    private static final String DB_URL = "jdbc:mysql://localhost:3306/psgtech_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";  // Change if you use different username
    private static final String DB_PASSWORD = "qwerty";  // ⚠️ CHANGE THIS TO YOUR MYSQL PASSWORD!

    private DatabaseManager() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            Properties props = new Properties();
            props.setProperty("user", DB_USER);
            props.setProperty("password", DB_PASSWORD);

            connection = DriverManager.getConnection(DB_URL, props);
            System.out.println("✅ MySQL database connection established successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to MySQL database!");
            System.err.println("   Check your credentials in DatabaseManager.java");
            System.err.println("   DB_USER: " + DB_USER);
            System.err.println("   DB_URL: " + DB_URL);
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Properties props = new Properties();
                props.setProperty("user", DB_USER);
                props.setProperty("password", DB_PASSWORD);
                connection = DriverManager.getConnection(DB_URL, props);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to reconnect to database!");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Initialize database tables for MySQL
     */
    public void initializeDatabase() {
        try {
            Statement stmt = connection.createStatement();

            // Students table
            String createStudentsTable = """
                CREATE TABLE IF NOT EXISTS students (
                    roll_no VARCHAR(20) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    date_of_birth DATE,
                    department VARCHAR(50),
                    batch VARCHAR(10),
                    current_semester INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.execute(createStudentsTable);
            System.out.println("✅ Table 'students' created/verified");

            // Courses table
            String createCoursesTable = """
                CREATE TABLE IF NOT EXISTS courses (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    roll_no VARCHAR(20),
                    semester INT NOT NULL,
                    course_code VARCHAR(20) NOT NULL,
                    course_name VARCHAR(200) NOT NULL,
                    credits INT NOT NULL,
                    grade VARCHAR(5),
                    grade_points DECIMAL(3,1),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_course (roll_no, course_code, semester),
                    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.execute(createCoursesTable);
            System.out.println("✅ Table 'courses' created/verified");

            // Internal Marks table
            // Update your internal_marks table creation in DatabaseManager.java
// Find the createTables() method and replace the internal_marks creation with this:

            String createInternalMarksTable = """
    CREATE TABLE IF NOT EXISTS internal_marks (
        id INT AUTO_INCREMENT PRIMARY KEY,
        roll_no VARCHAR(20) NOT NULL,
        semester INT NOT NULL,
        course_code VARCHAR(20) NOT NULL,
        course_name VARCHAR(255),
        total_internal_marks DOUBLE,
        max_marks DOUBLE NOT NULL DEFAULT 50.0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY unique_internal (roll_no, semester, course_code),
        INDEX idx_rollno (roll_no),
        INDEX idx_semester (semester)
    )
""";
            stmt.execute(createInternalMarksTable);
            System.out.println("✅ Table 'internal_marks' created/verified");
            // End Semester Marks table
            String createEndSemMarksTable = """
                CREATE TABLE IF NOT EXISTS endsem_marks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    roll_no VARCHAR(20),
                    semester INT NOT NULL,
                    course_code VARCHAR(20) NOT NULL,
                    course_name VARCHAR(200) NOT NULL,
                    endsem_marks DECIMAL(5,2),
                    max_marks DECIMAL(5,2) DEFAULT 100,
                    final_marks DECIMAL(5,2),
                    grade VARCHAR(5),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_endsem (roll_no, course_code, semester),
                    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.execute(createEndSemMarksTable);
            System.out.println("✅ Table 'endsem_marks' created/verified");

            // CGPA History table
            String createCGPATable = """
                CREATE TABLE IF NOT EXISTS cgpa_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    roll_no VARCHAR(20),
                    semester INT NOT NULL,
                    gpa DECIMAL(4,3),
                    cgpa DECIMAL(4,3),
                    total_credits INT,
                    has_backlogs BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_cgpa (roll_no, semester),
                    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.execute(createCGPATable);
            System.out.println("✅ Table 'cgpa_history' created/verified");

            // ML Training Data table (for performance prediction)
            String createMLDataTable = """
                CREATE TABLE IF NOT EXISTS ml_training_data (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    roll_no VARCHAR(20),
                    semester INT NOT NULL,
                    course_code VARCHAR(20) NOT NULL,
                    internal_marks DECIMAL(5,2),
                    endsem_marks DECIMAL(5,2),
                    final_marks DECIMAL(5,2),
                    class_average_internal DECIMAL(5,2),
                    class_average_endsem DECIMAL(5,2),
                    percentile_rank DECIMAL(5,2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE,
                    INDEX idx_course (course_code),
                    INDEX idx_semester (semester)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.execute(createMLDataTable);
            System.out.println("✅ Table 'ml_training_data' created/verified");

            // Performance Analytics table
            String createAnalyticsTable = """
                CREATE TABLE IF NOT EXISTS performance_analytics (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    roll_no VARCHAR(20),
                    semester INT NOT NULL,
                    course_code VARCHAR(20) NOT NULL,
                    predicted_endsem_score DECIMAL(5,2),
                    improvement_needed DECIMAL(5,2),
                    class_percentile DECIMAL(5,2),
                    recommendation TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_analytics (roll_no, course_code, semester),
                    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.execute(createAnalyticsTable);
            System.out.println("✅ Table 'performance_analytics' created/verified");

            System.out.println("\n🎉 All database tables initialized successfully!");
            stmt.close();

        } catch (SQLException e) {
            System.err.println("❌ Error initializing database tables!");
            e.printStackTrace();
        }
    }

    /**
     * Test database connection
     */
    public boolean testConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                // Execute a simple query to test
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1");
                boolean hasResult = rs.next();
                rs.close();
                stmt.close();
                return hasResult;
            }
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get database metadata info
     */
    public void printDatabaseInfo() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("\n📊 Database Information:");
            System.out.println("   Product Name: " + metaData.getDatabaseProductName());
            System.out.println("   Product Version: " + metaData.getDatabaseProductVersion());
            System.out.println("   Driver Name: " + metaData.getDriverName());
            System.out.println("   Driver Version: " + metaData.getDriverVersion());
            System.out.println("   URL: " + metaData.getURL());
            System.out.println("   User: " + metaData.getUserName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * List all tables in database
     */
    public void listTables() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            System.out.println("\n📋 Tables in database:");
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("   - " + tableName);
            }
            tables.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed successfully.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing database connection!");
            e.printStackTrace();
        }
    }

    /**
     * Main method for testing database connection
     */
    public static void main(String[] args) {
        System.out.println("🔄 Testing MySQL Database Connection...\n");

        DatabaseManager dbManager = DatabaseManager.getInstance();

        if (dbManager.testConnection()) {
            System.out.println("✅ Connection test passed!");
            dbManager.printDatabaseInfo();
            dbManager.initializeDatabase();
            dbManager.listTables();
        } else {
            System.err.println("❌ Connection test failed!");
            System.err.println("\n⚠️  Please check:");
            System.err.println("   1. MySQL is running");
            System.err.println("   2. Database 'psgtech_portal' exists");
            System.err.println("   3. Username and password are correct");
            System.err.println("   4. MySQL Connector dependency is in pom.xml");
        }

        dbManager.closeConnection();
    }
}