package com.campus.tracker.dao;

import com.campus.tracker.model.Attendance;
import com.campus.tracker.util.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public boolean addAttendance(Attendance attendance) {
        String sql = "INSERT INTO attendance (subject_id, classes_attended, total_classes, percentage) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, attendance.getSubjectId());
            stmt.setInt(2, attendance.getClassesAttended());
            stmt.setInt(3, attendance.getTotalClasses());
            stmt.setDouble(4, attendance.getPercentage());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    attendance.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Attendance> getAttendanceByStudent(int studentId) {
        List<Attendance> attendanceList = new ArrayList<>();
        String sql = "SELECT a.* FROM attendance a JOIN subjects s ON a.subject_id = s.id WHERE s.student_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Attendance attendance = new Attendance();
                attendance.setId(rs.getInt("id"));
                attendance.setSubjectId(rs.getInt("subject_id"));
                attendance.setClassesAttended(rs.getInt("classes_attended"));
                attendance.setTotalClasses(rs.getInt("total_classes"));
                attendance.setPercentage(rs.getDouble("percentage"));
                attendanceList.add(attendance);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendanceList;
    }

    public Attendance getBySubjectId(int subjectId) {
        String sql = "SELECT * FROM attendance WHERE subject_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subjectId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Attendance attendance = new Attendance();
                attendance.setId(rs.getInt("id"));
                attendance.setSubjectId(rs.getInt("subject_id"));
                attendance.setClassesAttended(rs.getInt("classes_attended"));
                attendance.setTotalClasses(rs.getInt("total_classes"));
                attendance.setPercentage(rs.getDouble("percentage"));
                return attendance;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateAttendance(Attendance attendance) {
        String sql = "UPDATE attendance SET classes_attended = ?, total_classes = ?, percentage = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, attendance.getClassesAttended());
            stmt.setInt(2, attendance.getTotalClasses());
            stmt.setDouble(3, attendance.getPercentage());
            stmt.setInt(4, attendance.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}