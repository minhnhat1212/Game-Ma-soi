-- Fix bảng matches nếu đã tạo trước đó
-- Chạy script này nếu bảng matches đã tồn tại nhưng có lỗi foreign key

USE werewolf_game;

-- Xóa bảng matches nếu đã tồn tại (sẽ xóa cả foreign keys)
DROP TABLE IF EXISTS matches;

-- Tạo lại bảng matches với cấu hình đúng
CREATE TABLE matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NULL,
    winner_team ENUM('VILLAGERS', 'WEREWOLVES') NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NULL,
    total_rounds INT DEFAULT 0,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL,
    INDEX idx_room (room_id),
    INDEX idx_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
