package com.campus.tracker.dao;

import com.campus.tracker.model.Student;
import com.campus.tracker.util.DatabaseConfig;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Map;

public class StudentDAO {

    public boolean register(String username, String password, String name, String email) {
        String sql = "INSERT INTO students (username, password_hash, name, email) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, name);
            stmt.setString(4, email);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // In your StudentDAO class
    public void saveScrapedData(int studentId, Map<String, Object> data) {
        String sql = "INSERT INTO scraped_data (student_id, name, cgpa, attendance_json, subjects_json, scraped_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE " +
                "name=VALUES(name), cgpa=VALUES(cgpa), attendance_json=VALUES(attendance_json), " +
                "subjects_json=VALUES(subjects_json), scraped_at=NOW()";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ps.setString(2, (String) data.get("name"));
            ps.setDouble(3, (Double) data.get("cgpa"));

            Gson gson = new Gson();
            ps.setString(4, gson.toJson(data.get("attendance")));
            ps.setString(5, gson.toJson(data.get("subjects")));

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Student login(String username, String password) {
        String sql = "SELECT * FROM students WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    Student student = new Student();
                    student.setId(rs.getInt("id"));
                    student.setUsername(rs.getString("username"));
                    student.setName(rs.getString("name"));
                    student.setEmail(rs.getString("email"));
                    return student;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Student getByUsername(String username) {
        String sql = "SELECT * FROM students WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setUsername(rs.getString("username"));
                student.setName(rs.getString("name"));
                student.setEmail(rs.getString("email"));
                return student;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Student getById(int studentId) {
        String sql = "SELECT * FROM students WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setUsername(rs.getString("username"));
                student.setName(rs.getString("name"));
                student.setEmail(rs.getString("email"));
                return student;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}