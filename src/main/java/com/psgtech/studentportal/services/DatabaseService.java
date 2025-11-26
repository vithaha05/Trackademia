package com.psgtech.studentportal.services;

import com.psgtech.studentportal.models.*;
import com.psgtech.studentportal.database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Service Layer
 * Handles all database CRUD operations
 */
public class DatabaseService {

    private DatabaseManager dbManager;

    public DatabaseService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Save or update student information
     */
    public void saveStudent(Student student) throws SQLException {
        String sql = """
            INSERT INTO students (roll_no, name, date_of_birth, department, batch, current_semester)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                date_of_birth = VALUES(date_of_birth),
                department = VALUES(department),
                batch = VALUES(batch),
                current_semester = VALUES(current_semester),
                updated_at = CURRENT_TIMESTAMP
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, student.getRollNo());
            stmt.setString(2, student.getName());
            stmt.setDate(3, student.getDateOfBirth() != null ?
                    Date.valueOf(student.getDateOfBirth()) : null);
            stmt.setString(4, student.getDepartment());
            stmt.setString(5, student.getBatch());
            stmt.setInt(6, student.getCurrentSemester());
            stmt.executeUpdate();
            System.out.println("✅ Student saved: " + student.getRollNo());
        }
    }

    /**
     * Save or update internal marks
     */
    public void saveInternalMarks(InternalMarks internal) throws SQLException {
        String sql = """
            INSERT INTO internal_marks 
            (roll_no, semester, course_code, course_name, total_internal_marks, max_marks)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                total_internal_marks = VALUES(total_internal_marks),
                updated_at = CURRENT_TIMESTAMP
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, internal.getRollNo());
            stmt.setInt(2, internal.getSemester());
            stmt.setString(3, internal.getCourseCode());
            stmt.setString(4, internal.getCourseName());

            if (internal.getTotalInternalMarks() != null) {
                stmt.setDouble(5, internal.getTotalInternalMarks());
            } else {
                stmt.setNull(5, Types.DOUBLE);
            }

            stmt.setDouble(6, internal.getMaxMarks());
            stmt.executeUpdate();
        }
    }

    /**
     * Save or update end semester marks
     */
    public void saveEndSemMarks(EndSemMarks endsem) throws SQLException {
        String sql = """
            INSERT INTO endsem_marks 
            (roll_no, semester, course_code, course_name, endsem_marks, 
             max_marks, final_marks, grade)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                endsem_marks = VALUES(endsem_marks),
                final_marks = VALUES(final_marks),
                grade = VALUES(grade),
                updated_at = CURRENT_TIMESTAMP
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, endsem.getRollNo());
            stmt.setInt(2, endsem.getSemester());
            stmt.setString(3, endsem.getCourseCode());
            stmt.setString(4, endsem.getCourseName());

            if (endsem.getEndsemMarks() != null) {
                stmt.setDouble(5, endsem.getEndsemMarks());
            } else {
                stmt.setNull(5, Types.DOUBLE);
            }

            stmt.setDouble(6, endsem.getMaxMarks());

            if (endsem.getFinalMarks() != null) {
                stmt.setDouble(7, endsem.getFinalMarks());
            } else {
                stmt.setNull(7, Types.DOUBLE);
            }

            stmt.setString(8, endsem.getGrade());
            stmt.executeUpdate();
        }
    }

    /**
     * Save course information
     */
    public void saveCourse(Course course) throws SQLException {
        String sql = """
            INSERT INTO courses 
            (roll_no, semester, course_code, course_name, credits, grade, grade_points)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                grade = VALUES(grade),
                grade_points = VALUES(grade_points)
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, course.getRollNo());
            stmt.setInt(2, course.getSemester());
            stmt.setString(3, course.getCourseCode());
            stmt.setString(4, course.getCourseName());
            stmt.setInt(5, course.getCredits());
            stmt.setString(6, course.getGrade());
            stmt.setDouble(7, course.getGradePoints());
            stmt.executeUpdate();
        }
    }

    /**
     * Save CGPA record
     */
    public void saveCGPARecord(CGPARecord record) throws SQLException {
        String sql = """
            INSERT INTO cgpa_history 
            (roll_no, semester, gpa, cgpa, total_credits, has_backlogs)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                gpa = VALUES(gpa),
                cgpa = VALUES(cgpa),
                total_credits = VALUES(total_credits),
                has_backlogs = VALUES(has_backlogs)
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, record.getRollNo());
            stmt.setInt(2, record.getSemester());

            if (record.getGpa() != null) {
                stmt.setDouble(3, record.getGpa());
            } else {
                stmt.setNull(3, Types.DOUBLE);
            }

            if (record.getCgpa() != null) {
                stmt.setDouble(4, record.getCgpa());
            } else {
                stmt.setNull(4, Types.DOUBLE);
            }

            if (record.getTotalCredits() != null) {
                stmt.setInt(5, record.getTotalCredits());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setBoolean(6, record.isHasBacklogs());
            stmt.executeUpdate();
        }
    }

    /**
     * Get all internal marks for a student
     */
    public List<InternalMarks> getInternalMarks(String rollNo) throws SQLException {
        String sql = """
            SELECT * FROM internal_marks 
            WHERE roll_no = ? 
            ORDER BY semester DESC, course_code
        """;

        List<InternalMarks> internalsList = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                InternalMarks internal = new InternalMarks();
                internal.setId(rs.getInt("id"));
                internal.setRollNo(rs.getString("roll_no"));
                internal.setSemester(rs.getInt("semester"));
                internal.setCourseCode(rs.getString("course_code"));
                internal.setCourseName(rs.getString("course_name"));

                double totalMarks = rs.getDouble("total_internal_marks");
                if (!rs.wasNull()) {
                    internal.setTotalInternalMarks(totalMarks);
                }

                internal.setMaxMarks(rs.getDouble("max_marks"));
                internalsList.add(internal);
            }
            rs.close();
        }

        return internalsList;
    }

    /**
     * Get all end semester marks for a student
     */
    public List<EndSemMarks> getEndSemMarks(String rollNo) throws SQLException {
        String sql = """
            SELECT * FROM endsem_marks 
            WHERE roll_no = ? 
            ORDER BY semester DESC, course_code
        """;

        List<EndSemMarks> endsemList = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                EndSemMarks endsem = new EndSemMarks();
                endsem.setId(rs.getInt("id"));
                endsem.setRollNo(rs.getString("roll_no"));
                endsem.setSemester(rs.getInt("semester"));
                endsem.setCourseCode(rs.getString("course_code"));
                endsem.setCourseName(rs.getString("course_name"));

                double endsemMarks = rs.getDouble("endsem_marks");
                if (!rs.wasNull()) {
                    endsem.setEndsemMarks(endsemMarks);
                }

                endsem.setMaxMarks(rs.getDouble("max_marks"));

                double finalMarks = rs.getDouble("final_marks");
                if (!rs.wasNull()) {
                    endsem.setFinalMarks(finalMarks);
                }

                endsem.setGrade(rs.getString("grade"));
                endsemList.add(endsem);
            }
            rs.close();
        }

        return endsemList;
    }

    /**
     * Get CGPA history for a student
     */
    public List<CGPARecord> getCGPAHistory(String rollNo) throws SQLException {
        String sql = """
            SELECT * FROM cgpa_history 
            WHERE roll_no = ? 
            ORDER BY semester
        """;

        List<CGPARecord> cgpaList = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                CGPARecord record = new CGPARecord();
                record.setId(rs.getInt("id"));
                record.setRollNo(rs.getString("roll_no"));
                record.setSemester(rs.getInt("semester"));

                double gpa = rs.getDouble("gpa");
                if (!rs.wasNull()) {
                    record.setGpa(gpa);
                }

                double cgpa = rs.getDouble("cgpa");
                if (!rs.wasNull()) {
                    record.setCgpa(cgpa);
                }

                int credits = rs.getInt("total_credits");
                if (!rs.wasNull()) {
                    record.setTotalCredits(credits);
                }

                record.setHasBacklogs(rs.getBoolean("has_backlogs"));
                cgpaList.add(record);
            }
            rs.close();
        }

        return cgpaList;
    }

    /**
     * Get all courses for a student
     */
    public List<Course> getCourses(String rollNo) throws SQLException {
        String sql = """
            SELECT * FROM courses 
            WHERE roll_no = ? 
            ORDER BY semester, course_code
        """;

        List<Course> coursesList = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Course course = new Course();
                course.setId(rs.getInt("id"));
                course.setRollNo(rs.getString("roll_no"));
                course.setSemester(rs.getInt("semester"));
                course.setCourseCode(rs.getString("course_code"));
                course.setCourseName(rs.getString("course_name"));
                course.setCredits(rs.getInt("credits"));
                course.setGrade(rs.getString("grade"));
                course.setGradePoints(rs.getDouble("grade_points"));
                coursesList.add(course);
            }
            rs.close();
        }

        return coursesList;
    }

    /**
     * Save ML training data
     */
    public void saveMLTrainingData(String rollNo, int semester, String courseCode,
                                   double internalMarks, double endsemMarks,
                                   double finalMarks) throws SQLException {
        String sql = """
            INSERT INTO ml_training_data 
            (roll_no, semester, course_code, internal_marks, endsem_marks, final_marks)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            stmt.setInt(2, semester);
            stmt.setString(3, courseCode);
            stmt.setDouble(4, internalMarks);
            stmt.setDouble(5, endsemMarks);
            stmt.setDouble(6, finalMarks);
            stmt.executeUpdate();
        }
    }

    /**
     * Get student by roll number
     */
    public Student getStudent(String rollNo) throws SQLException {
        String sql = "SELECT * FROM students WHERE roll_no = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Student student = new Student();
                student.setRollNo(rs.getString("roll_no"));
                student.setName(rs.getString("name"));

                Date dob = rs.getDate("date_of_birth");
                if (dob != null) {
                    student.setDateOfBirth(dob.toLocalDate());
                }

                student.setDepartment(rs.getString("department"));
                student.setBatch(rs.getString("batch"));
                student.setCurrentSemester(rs.getInt("current_semester"));
                rs.close();
                return student;
            }
            rs.close();
        }

        return null;
    }
}