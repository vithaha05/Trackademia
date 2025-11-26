package com.campus.tracker.dao;

import com.campus.tracker.model.Subject;
import com.campus.tracker.util.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    public boolean addSubject(Subject subject) {
        String sql = "INSERT INTO subjects (student_id, subject_code, subject_name, credits, semester) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, subject.getStudentId());
            stmt.setString(2, subject.getSubjectCode());
            stmt.setString(3, subject.getSubjectName());
            stmt.setInt(4, subject.getCredits());
            stmt.setInt(5, subject.getSemester());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    subject.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Subject> getSubjectsByStudent(int studentId) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM subjects WHERE student_id = ? ORDER BY semester, subject_name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Subject subject = new Subject();
                subject.setId(rs.getInt("id"));
                subject.setStudentId(rs.getInt("student_id"));
                subject.setSubjectCode(rs.getString("subject_code"));
                subject.setSubjectName(rs.getString("subject_name"));
                subject.setCredits(rs.getInt("credits"));
                subject.setSemester(rs.getInt("semester"));
                subjects.add(subject);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subjects;
    }

    public Subject getById(int subjectId) {
        String sql = "SELECT * FROM subjects WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subjectId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Subject subject = new Subject();
                subject.setId(rs.getInt("id"));
                subject.setStudentId(rs.getInt("student_id"));
                subject.setSubjectCode(rs.getString("subject_code"));
                subject.setSubjectName(rs.getString("subject_name"));
                subject.setCredits(rs.getInt("credits"));
                subject.setSemester(rs.getInt("semester"));
                return subject;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteSubject(int subjectId) {
        String sql = "DELETE FROM subjects WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subjectId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}