package com.werewolf.server.repository;

import com.werewolf.server.config.DatabaseConfig;
import com.werewolf.server.entity.Room;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Repository để thao tác với bảng rooms
 */
public class RoomRepository {
    
    public Room create(Room room) {
        String sql = "INSERT INTO rooms (name, password_hash, host_user_id, max_players, phase_duration_seconds, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, room.getName());
            if (room.getPasswordHash() != null) {
                stmt.setString(2, room.getPasswordHash());
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            stmt.setInt(3, room.getHostUserId());
            stmt.setInt(4, room.getMaxPlayers());
            stmt.setInt(5, room.getPhaseDurationSeconds());
            stmt.setString(6, room.getStatus());
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    room.setId(rs.getInt(1));
                }
            }
            return room;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo room", e);
        }
    }

    public Room findById(int id) {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRoom(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm room", e);
        }
        return null;
    }

    public void updateStatus(int roomId, String status) {
        String sql = "UPDATE rooms SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật status room", e);
        }
    }

    public void updateStartedAt(int roomId, LocalDateTime startedAt) {
        String sql = "UPDATE rooms SET started_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startedAt));
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật started_at", e);
        }
    }

    public void updateEndedAt(int roomId, LocalDateTime endedAt) {
        String sql = "UPDATE rooms SET ended_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(endedAt));
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật ended_at", e);
        }
    }

    public void deleteById(int roomId) {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa room", e);
        }
    }

    public void updateHost(int roomId, int hostUserId) {
        String sql = "UPDATE rooms SET host_user_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, hostUserId);
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật host", e);
        }
    }

    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getInt("id"));
        room.setName(rs.getString("name"));
        room.setPasswordHash(rs.getString("password_hash"));
        room.setHostUserId(rs.getInt("host_user_id"));
        room.setMaxPlayers(rs.getInt("max_players"));
        room.setPhaseDurationSeconds(rs.getInt("phase_duration_seconds"));
        room.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            room.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            room.setStartedAt(startedAt.toLocalDateTime());
        }
        Timestamp endedAt = rs.getTimestamp("ended_at");
        if (endedAt != null) {
            room.setEndedAt(endedAt.toLocalDateTime());
        }
        return room;
    }
}
