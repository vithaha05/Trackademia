package com.campus.tracker.dao;

import com.campus.tracker.model.CAMarks;
import com.campus.tracker.util.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CAMarksDAO {

    public boolean addCAMarks(CAMarks caMarks) {
        String sql = "INSERT INTO ca_marks (subject_id, t1, t2, rt, rt1, rt2, ap, mp1, mp2, total, conv_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, caMarks.getSubjectId());
            stmt.setString(2, caMarks.getT1());
            stmt.setString(3, caMarks.getT2());
            stmt.setString(4, caMarks.getRt());
            stmt.setString(5, caMarks.getRt1());
            stmt.setString(6, caMarks.getRt2());
            stmt.setString(7, caMarks.getAp());
            stmt.setString(8, caMarks.getMp1());
            stmt.setString(9, caMarks.getMp2());
            stmt.setString(10, caMarks.getTotal());
            stmt.setString(11, caMarks.getConvTotal());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    caMarks.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public CAMarks getBySubjectId(int subjectId) {
        String sql = "SELECT * FROM ca_marks WHERE subject_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subjectId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                CAMarks ca = new CAMarks();
                ca.setId(rs.getInt("id"));
                ca.setSubjectId(rs.getInt("subject_id"));
                ca.setT1(rs.getString("t1"));
                ca.setT2(rs.getString("t2"));
                ca.setRt(rs.getString("rt"));
                ca.setRt1(rs.getString("rt1"));
                ca.setRt2(rs.getString("rt2"));
                ca.setAp(rs.getString("ap"));
                ca.setMp1(rs.getString("mp1"));
                ca.setMp2(rs.getString("mp2"));
                ca.setTotal(rs.getString("total"));
                ca.setConvTotal(rs.getString("conv_total"));
                return ca;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<CAMarks> getByStudentId(int studentId) {
        List<CAMarks> caMarksList = new ArrayList<>();
        String sql = "SELECT c.* FROM ca_marks c JOIN subjects s ON c.subject_id = s.id WHERE s.student_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                CAMarks ca = new CAMarks();
                ca.setId(rs.getInt("id"));
                ca.setSubjectId(rs.getInt("subject_id"));
                ca.setT1(rs.getString("t1"));
                ca.setT2(rs.getString("t2"));
                ca.setRt(rs.getString("rt"));
                ca.setRt1(rs.getString("rt1"));
                ca.setRt2(rs.getString("rt2"));
                ca.setAp(rs.getString("ap"));
                ca.setMp1(rs.getString("mp1"));
                ca.setMp2(rs.getString("mp2"));
                ca.setTotal(rs.getString("total"));
                ca.setConvTotal(rs.getString("conv_total"));
                caMarksList.add(ca);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return caMarksList;
    }

    public boolean updateCAMarks(CAMarks caMarks) {
        String sql = "UPDATE ca_marks SET t1 = ?, t2 = ?, rt = ?, rt1 = ?, rt2 = ?, " +
                "ap = ?, mp1 = ?, mp2 = ?, total = ?, conv_total = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, caMarks.getT1());
            stmt.setString(2, caMarks.getT2());
            stmt.setString(3, caMarks.getRt());
            stmt.setString(4, caMarks.getRt1());
            stmt.setString(5, caMarks.getRt2());
            stmt.setString(6, caMarks.getAp());
            stmt.setString(7, caMarks.getMp1());
            stmt.setString(8, caMarks.getMp2());
            stmt.setString(9, caMarks.getTotal());
            stmt.setString(10, caMarks.getConvTotal());
            stmt.setInt(11, caMarks.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}