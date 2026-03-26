package com.werewolf.server.repository;

import com.werewolf.server.config.DatabaseConfig;
import com.werewolf.server.entity.User;

import java.sql.*;
import java.util.Set;

/**
 * Repository để thao tác với bảng users
 * Sử dụng JDBC thuần (không dùng JPA để đơn giản hóa)
 */
public class UserRepository {
    
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm user", e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm user", e);
        }
        return null;
    }

    public User create(User user) {
        String sql = "INSERT INTO users (username, password_hash, display_name, avatar, is_online) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getDisplayName());
            stmt.setString(4, user.getAvatar());
            stmt.setBoolean(5, user.isOnline());
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo user", e);
        }
    }

    public void applyMatchRewards(Set<Integer> winnerUserIds, Set<Integer> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            return;
        }
        final int loseXp = 20;
        final int winXp = 50;
        final int loseCoins = 10;
        final int winCoins = 30;

        String sql = "UPDATE users SET " +
                "games_played = games_played + 1, " +
                "games_won = games_won + ?, " +
                "experience = experience + ?, " +
                "coins = coins + ?, " +
                "level = 1 + FLOOR((experience + ?) / 100) " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Integer userId : participantUserIds) {
                boolean isWinner = winnerUserIds != null && winnerUserIds.contains(userId);
                int xpGain = isWinner ? winXp : loseXp;
                int coinGain = isWinner ? winCoins : loseCoins;
                stmt.setInt(1, isWinner ? 1 : 0);
                stmt.setInt(2, xpGain);
                stmt.setInt(3, coinGain);
                stmt.setInt(4, xpGain);
                stmt.setInt(5, userId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật phần thưởng trận đấu", e);
        }
    }

    public void updateOnlineStatus(int userId, boolean online) {
        String sql = "UPDATE users SET is_online = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, online);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật trạng thái online", e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));
        user.setAvatar(rs.getString("avatar"));
        user.setOnline(rs.getBoolean("is_online"));
        user.setLevel(getIntOrDefault(rs, "level", 1));
        user.setExperience(getIntOrDefault(rs, "experience", 0));
        user.setCoins(getIntOrDefault(rs, "coins", 0));
        user.setGamesPlayed(getIntOrDefault(rs, "games_played", 0));
        user.setGamesWon(getIntOrDefault(rs, "games_won", 0));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    }

    private int getIntOrDefault(ResultSet rs, String column, int defaultValue) {
        try {
            return rs.getInt(column);
        } catch (SQLException ignored) {
            return defaultValue;
        }
    }
}
