package com.campus.tracker.dao;

import com.campus.tracker.model.Grade;
import com.campus.tracker.util.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeDAO {

    public boolean addGrade(Grade grade) {
        String sql = "INSERT INTO grades (subject_id, internal_marks, external_marks, total_marks, grade_point, grade) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, grade.getSubjectId());
            stmt.setDouble(2, grade.getInternalMarks());
            stmt.setDouble(3, grade.getExternalMarks());
            stmt.setDouble(4, grade.getTotalMarks());
            stmt.setDouble(5, grade.getGradePoint());
            stmt.setString(6, grade.getGrade());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    grade.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Grade> getGradesByStudent(int studentId) {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT g.* FROM grades g JOIN subjects s ON g.subject_id = s.id WHERE s.student_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Grade grade = new Grade();
                grade.setId(rs.getInt("id"));
                grade.setSubjectId(rs.getInt("subject_id"));
                grade.setInternalMarks(rs.getDouble("internal_marks"));
                grade.setExternalMarks(rs.getDouble("external_marks"));
                grade.setTotalMarks(rs.getDouble("total_marks"));
                grade.setGradePoint(rs.getDouble("grade_point"));
                grade.setGrade(rs.getString("grade"));
                grades.add(grade);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grades;
    }

    public Grade getBySubjectId(int subjectId) {
        String sql = "SELECT * FROM grades WHERE subject_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subjectId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Grade grade = new Grade();
                grade.setId(rs.getInt("id"));
                grade.setSubjectId(rs.getInt("subject_id"));
                grade.setInternalMarks(rs.getDouble("internal_marks"));
                grade.setExternalMarks(rs.getDouble("external_marks"));
                grade.setTotalMarks(rs.getDouble("total_marks"));
                grade.setGradePoint(rs.getDouble("grade_point"));
                grade.setGrade(rs.getString("grade"));
                return grade;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateGrade(Grade grade) {
        String sql = "UPDATE grades SET internal_marks = ?, external_marks = ?, total_marks = ?, grade_point = ?, grade = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, grade.getInternalMarks());
            stmt.setDouble(2, grade.getExternalMarks());
            stmt.setDouble(3, grade.getTotalMarks());
            stmt.setDouble(4, grade.getGradePoint());
            stmt.setString(5, grade.getGrade());
            stmt.setInt(6, grade.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}