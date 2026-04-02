package com.werewolf.server.repository;

import com.werewolf.server.config.DatabaseConfig;
import com.werewolf.server.entity.User;
import com.werewolf.server.service.ProgressionService;

import java.sql.*;
import java.util.Map;

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

    /**
     * Áp dụng phần thưởng chi tiết (dùng với ProgressionService)
     */
    public void applyMatchRewardsDetailed(Map<Integer, ProgressionService.RewardResult> rewards) {
        if (rewards == null || rewards.isEmpty()) return;

        // Công thức level: levelFromTotalXp() dùng RankSystem
        // Cập nhật: experience += totalXp, coins += coins, games_played++, games_won += (won?1:0)
        // Sau đó tính lại level từ experience mới
        String sql = "UPDATE users SET " +
                "games_played = games_played + 1, " +
                "games_won = games_won + ?, " +
                "experience = experience + ?, " +
                "coins = coins + ?, " +
                "level = CASE " +
                "  WHEN (experience + ?) < 100   THEN 1 " +
                "  WHEN (experience + ?) < 300   THEN 2 " +
                "  WHEN (experience + ?) < 600   THEN 3 " +
                "  WHEN (experience + ?) < 1000  THEN 4 " +
                "  WHEN (experience + ?) < 1500  THEN 5 " +
                "  WHEN (experience + ?) < 2100  THEN 6 " +
                "  WHEN (experience + ?) < 2800  THEN 7 " +
                "  WHEN (experience + ?) < 3600  THEN 8 " +
                "  WHEN (experience + ?) < 4500  THEN 9 " +
                "  WHEN (experience + ?) < 5500  THEN 10 " +
                "  WHEN (experience + ?) < 7000  THEN 11 " +
                "  WHEN (experience + ?) < 8700  THEN 12 " +
                "  WHEN (experience + ?) < 10600 THEN 13 " +
                "  WHEN (experience + ?) < 12700 THEN 14 " +
                "  WHEN (experience + ?) < 15000 THEN 15 " +
                "  WHEN (experience + ?) < 20000 THEN 20 " +
                "  WHEN (experience + ?) < 30000 THEN 30 " +
                "  ELSE 50 " +
                "END " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ProgressionService.RewardResult r : rewards.values()) {
                int totalXp = r.getTotalXp();
                int paramIdx = 1;
                stmt.setInt(paramIdx++, r.won ? 1 : 0);
                stmt.setInt(paramIdx++, totalXp);
                stmt.setInt(paramIdx++, r.coinsGained);
                // Level CASE: 19 cột xp cần điền
                for (int i = 0; i < 19; i++) {
                    stmt.setInt(paramIdx++, totalXp);
                }
                stmt.setInt(paramIdx, r.userId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật phần thưởng chi tiết", e);
        }
    }

    /**
     * Lấy XP hiện tại của user (dùng sau khi cập nhật để tính rank mới)
     */
    public int getExperienceById(int userId) {
        String sql = "SELECT experience FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("experience");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy XP", e);
        }
        return 0;
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
