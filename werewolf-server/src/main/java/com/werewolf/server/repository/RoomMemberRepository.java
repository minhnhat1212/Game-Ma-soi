package com.werewolf.server.repository;

import com.werewolf.server.config.DatabaseConfig;

import java.sql.*;

/**
 * Repository để thao tác với bảng room_members
 */
public class RoomMemberRepository {
    
    public void addMember(int roomId, int userId) {
        String sql = "INSERT INTO room_members (room_id, user_id, is_ready) VALUES (?, ?, FALSE) ON DUPLICATE KEY UPDATE is_ready = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm member vào room", e);
        }
    }

    public void removeMember(int roomId, int userId) {
        String sql = "DELETE FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa member khỏi room", e);
        }
    }

    public void updateReady(int roomId, int userId, boolean ready) {
        String sql = "UPDATE room_members SET is_ready = ? WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, ready);
            stmt.setInt(2, roomId);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật ready status", e);
        }
    }

    public boolean isMember(int roomId, int userId) {
        String sql = "SELECT COUNT(*) FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra member", e);
        }
        return false;
    }
}
