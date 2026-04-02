-- ============================================
-- Hệ thống Level/Rank nâng cấp
-- Chạy script này nếu database đã tồn tại
-- ============================================

USE werewolf_game;

-- Thêm cột rank nếu chưa có (computed từ level)
ALTER TABLE users
    ADD COLUMN rank_name VARCHAR(30) AS (
        CASE
            WHEN level >= 50 THEN 'Huyen Thoai'
            WHEN level >= 30 THEN 'Lanh Chua'
            WHEN level >= 20 THEN 'Phu Thuy'
            WHEN level >= 15 THEN 'Chien Binh'
            WHEN level >= 10 THEN 'Tham Tu'
            WHEN level >= 5  THEN 'Dan Lang'
            ELSE 'Tan Binh'
        END
    ) STORED;

-- Cập nhật lại cột level cho tất cả user dựa theo XP mới
-- Công thức: dùng bảng ngưỡng XP
UPDATE users SET level = CASE
    WHEN experience < 100   THEN 1
    WHEN experience < 300   THEN 2
    WHEN experience < 600   THEN 3
    WHEN experience < 1000  THEN 4
    WHEN experience < 1500  THEN 5
    WHEN experience < 2100  THEN 6
    WHEN experience < 2800  THEN 7
    WHEN experience < 3600  THEN 8
    WHEN experience < 4500  THEN 9
    WHEN experience < 5500  THEN 10
    WHEN experience < 7000  THEN 11
    WHEN experience < 8700  THEN 12
    WHEN experience < 10600 THEN 13
    WHEN experience < 12700 THEN 14
    WHEN experience < 15000 THEN 15
    WHEN experience < 20000 THEN 20
    WHEN experience < 30000 THEN 30
    ELSE 50
END;

-- Cập nhật bảng match_players để hỗ trợ các vai trò mới
ALTER TABLE match_players
    MODIFY COLUMN role ENUM('VILLAGER', 'WEREWOLF', 'SEER', 'GUARD', 'WITCH', 'HUNTER') NOT NULL;

-- View tiện ích: xem thống kê người chơi kèm rank
CREATE OR REPLACE VIEW v_player_stats AS
SELECT
    u.id,
    u.username,
    u.display_name,
    u.level,
    u.experience,
    u.coins,
    u.games_played,
    u.games_won,
    ROUND(CASE WHEN u.games_played > 0 THEN u.games_won * 100.0 / u.games_played ELSE 0 END, 1) AS win_rate_pct,
    CASE
        WHEN u.level >= 50 THEN 'Huyen Thoai'
        WHEN u.level >= 30 THEN 'Lanh Chua'
        WHEN u.level >= 20 THEN 'Phu Thuy'
        WHEN u.level >= 15 THEN 'Chien Binh'
        WHEN u.level >= 10 THEN 'Tham Tu'
        WHEN u.level >= 5  THEN 'Dan Lang'
        ELSE 'Tan Binh'
    END AS rank_name
FROM users u
ORDER BY u.experience DESC;

-- Xem leaderboard top 10
-- SELECT * FROM v_player_stats LIMIT 10;
