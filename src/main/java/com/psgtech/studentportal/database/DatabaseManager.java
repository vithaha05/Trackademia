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
    private static final String DB_URL = "jdbc:mysql://localhost:3306/student_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root"; // Change if you use different username
    private static final String DB_PASSWORD = "qwerty"; // ⚠️ CHANGE THIS TO YOUR MYSQL PASSWORD!

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
                            program VARCHAR(100),
                            total_semesters INT DEFAULT 8,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;
            stmt.execute(createStudentsTable);

            // Add program columns if they don't exist (for existing tables)
            try {
                stmt.execute("ALTER TABLE students ADD COLUMN program VARCHAR(100)");
            } catch (SQLException e) {
                /* Column already exists */ }
            try {
                stmt.execute("ALTER TABLE students ADD COLUMN total_semesters INT DEFAULT 8");
            } catch (SQLException e) {
                /* Column already exists */ }

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
            // Find the createTables() method and replace the internal_marks creation with
            // this:

            String createInternalMarksTable = """
                        CREATE TABLE IF NOT EXISTS internal_marks (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            roll_no VARCHAR(20) NOT NULL,
                            semester INT NOT NULL,
                            course_code VARCHAR(20) NOT NULL,
                            course_name VARCHAR(255),
                            total_internal_marks DOUBLE,
                            attendance_percentage DOUBLE DEFAULT 0.0,
                            max_marks DOUBLE NOT NULL DEFAULT 50.0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            UNIQUE KEY unique_internal (roll_no, semester, course_code),
                            INDEX idx_rollno (roll_no),
                            INDEX idx_semester (semester)
                        )
                    """;
            stmt.execute(createInternalMarksTable);
            // Add attendance column if it doesn't exist (for existing tables)
            try {
                stmt.execute("ALTER TABLE internal_marks ADD COLUMN attendance_percentage DOUBLE DEFAULT 0.0");
            } catch (SQLException e) {
                /* Column already exists */ }

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
                            course_code VARCHAR(20) NOT NULL,
                            internal_marks DECIMAL(5,2),
                            attendance_percentage DOUBLE DEFAULT 0.0,
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
            try {
                stmt.execute("ALTER TABLE ml_training_data ADD COLUMN attendance_percentage DOUBLE DEFAULT 0.0");
            } catch (SQLException e) {
                /* Column already exists */ }

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

            // Populate sample ML training data for realistic predictions
            populateSampleMLData();

        } catch (SQLException e) {
            System.err.println("❌ Error initializing database tables!");
            e.printStackTrace();
        }
    }

    /**
     * Populate sample ML training data for realistic predictions
     * This creates synthetic historical data showing correlation between
     * internal marks and end-semester performance
     */
    public void populateSampleMLData() {
        System.out.println("\n🤖 Populating ML training data...");

        try {
            // First check if data already exists
            String checkSql = "SELECT COUNT(*) FROM ml_training_data";
            Statement checkStmt = connection.createStatement();
            ResultSet rs = checkStmt.executeQuery(checkSql);
            rs.next();
            int existingCount = rs.getInt(1);
            rs.close();
            checkStmt.close();

            if (existingCount > 50) {
                System.out.println("ℹ️ ML training data already populated (" + existingCount + " records)");
                return;
            }

            // Sample course patterns - covers theory and lab courses
            String[] courseCodes = {
                    "23XT51", "23XT52", "23XT53", "23XT54", "23XTE2", // Semester 5 theory
                    "23XT13", "23XT14", "23XT15", "23XT16", "23XT17", "23XT18", // Common courses
                    "22XT31", "22XT32", "22XT33", "22XT34", "22XT35", // Previous semesters
                    "21XT21", "21XT22", "21XT23", "21XT24"
            };

            String insertSql = """
                        INSERT INTO ml_training_data
                        (roll_no, semester, course_code, internal_marks, attendance_percentage, endsem_marks, final_marks)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE internal_marks = VALUES(internal_marks)
                    """;

            PreparedStatement pstmt = connection.prepareStatement(insertSql);
            int totalRecords = 0;

            // Generate realistic training data for each course
            java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility

            for (String courseCode : courseCodes) {
                // Determine semester from course code
                int semester = 5; // Default
                if (courseCode.startsWith("22"))
                    semester = 3;
                else if (courseCode.startsWith("21"))
                    semester = 2;
                else if (courseCode.contains("T1"))
                    semester = 1;
                else if (courseCode.contains("T2"))
                    semester = 2;
                else if (courseCode.contains("T3"))
                    semester = 3;
                else if (courseCode.contains("T4"))
                    semester = 4;
                else if (courseCode.contains("T5"))
                    semester = 5;

                // Generate 30-50 data points per course for good regression
                int dataPoints = 30 + random.nextInt(20);

                for (int i = 0; i < dataPoints; i++) {
                    // Generate synthetic roll numbers
                    String syntheticRollNo = "TRAIN" + String.format("%04d", totalRecords);

                    // Internal marks (out of 50) - realistic distribution
                    double internalMarks = generateRealisticInternalMarks(random);

                    // Attendance percentage (mostly high, 70-100)
                    double attendance = 70 + (random.nextDouble() * 30);
                    // Minimal attendance constraint
                    if (random.nextDouble() < 0.05)
                        attendance = 50 + (random.nextDouble() * 20); // Some low attendance

                    // End-sem marks correlate with internal but with noise
                    // Realistic correlation: students who do well in internal tend to do well in
                    // endsem
                    double endsemMarks = generateCorrelatedEndsemMarks(internalMarks, attendance, random);

                    // Final marks = weighted average (typically 40% internal + 60% endsem scaled)
                    double finalMarks = (internalMarks / 50.0 * 40.0) + (endsemMarks / 100.0 * 60.0);

                    pstmt.setString(1, syntheticRollNo);
                    pstmt.setInt(2, semester);
                    pstmt.setString(3, courseCode);
                    pstmt.setDouble(4, internalMarks);
                    pstmt.setDouble(5, attendance);
                    pstmt.setDouble(6, endsemMarks);
                    pstmt.setDouble(7, finalMarks);
                    pstmt.addBatch();

                    totalRecords++;
                }
            }

            pstmt.executeBatch();
            pstmt.close();

            System.out.println(
                    "✅ Populated " + totalRecords + " ML training records for " + courseCodes.length + " courses");

        } catch (SQLException e) {
            System.err.println("⚠️ Error populating ML training data: " + e.getMessage());
            // Non-fatal - app can still work without training data
        }
    }

    /**
     * Generate realistic internal marks distribution
     * Most students score between 25-45 out of 50
     */
    private double generateRealisticInternalMarks(java.util.Random random) {
        // Use a skewed distribution - most students do reasonably well
        double base = 30 + random.nextGaussian() * 8;
        // Clamp between 10 and 50
        return Math.max(10, Math.min(50, base));
    }

    /**
     * Generate end-sem marks correlated with internal marks and attendance
     * Correlation is not perfect - some students improve, others decline
     */
    private double generateCorrelatedEndsemMarks(double internalMarks, double attendance, java.util.Random random) {
        // Base prediction: scale internal to 100
        double expectedEndsem = (internalMarks / 50.0) * 100.0;

        // Attendance factor: High attendance boosts score slightly, Low attendance acts
        // as penalty
        // Attendance below 75% often indicates trouble
        if (attendance < 75) {
            expectedEndsem -= (75 - attendance) * 0.5; // Penalty
        } else {
            expectedEndsem += (attendance - 75) * 0.1; // Slight bonus for consistency
        }

        // Add realistic variation (standard deviation ~15)
        double noise = random.nextGaussian() * 15;

        // Some students improve significantly in endsem (10% chance)
        if (random.nextDouble() < 0.1) {
            noise += 15;
        }
        // Some students do worse (15% chance)
        if (random.nextDouble() < 0.15) {
            noise -= 10;
        }

        double endsemMarks = expectedEndsem + noise;

        // Clamp between 0 and 100
        return Math.max(0, Math.min(100, endsemMarks));
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
            ResultSet tables = metaData.getTables(null, null, "%", new String[] { "TABLE" });

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