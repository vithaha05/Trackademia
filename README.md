# Trackademia

Trackademia is a JavaFX application for tracking student academic performance, including attendance monitoring and ML-based predictions.

## Prerequisites

1.  **Java JDK 17** or higher.
2.  **Maven** installed (or use the wrapper if provided).
3.  **MySQL Server** running on `localhost:3306`.
4.  Database **`student_portal`** created.

## Database Setup

1.  Create the database:
    ```sql
    CREATE DATABASE IF NOT EXISTS student_portal;
    ```
2.  Configure credentials:
    Open `src/main/java/com/psgtech/studentportal/database/DatabaseManager.java` and update:
    ```java
    private static final String DB_USER = "root";       // Your MySQL username
    private static final String DB_PASSWORD = "qwerty"; // Your MySQL password
    ```

## Running the Application

To run the application, execute the following Maven command:

```bash
mvn clean javafx:run
```

## Features

- **Dashboard**: View CGPA and current semester status.
- **Attendance**: Enhanced with attendance percentage tracking.
- **Predictions**: ML-driven predictions for end-semester scores using internal marks and attendance.
