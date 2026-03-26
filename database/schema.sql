-- ============================================
-- Werewolf Game Database Schema
-- MySQL 8.0+
-- ============================================

CREATE DATABASE IF NOT EXISTS werewolf_game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE werewolf_game;

-- ============================================
-- Bảng Users
-- ============================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt hash',
    display_name VARCHAR(100) NOT NULL,
    avatar VARCHAR(255) NULL COMMENT 'URL hoặc path đến avatar',
    is_online BOOLEAN DEFAULT FALSE,
    level INT NOT NULL DEFAULT 1,
    experience INT NOT NULL DEFAULT 0,
    coins INT NOT NULL DEFAULT 0,
    games_played INT NOT NULL DEFAULT 0,
    games_won INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Bảng Rooms
-- ============================================
CREATE TABLE rooms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NULL COMMENT 'BCrypt hash, NULL nếu public',
    host_user_id INT NOT NULL,
    max_players INT NOT NULL DEFAULT 8 CHECK (max_players BETWEEN 4 AND 16),
    current_players INT DEFAULT 0,
    phase_duration_seconds INT DEFAULT 60 COMMENT 'Thời gian mỗi pha (giây)',
    status ENUM('WAITING', 'PLAYING', 'ENDED') DEFAULT 'WAITING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    FOREIGN KEY (host_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_host (host_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Bảng Room Members
-- ============================================
CREATE TABLE room_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    is_ready BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_room_user (room_id, user_id),
    INDEX idx_room (room_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Bảng Matches (Lịch sử trận đấu)
-- ============================================
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

-- ============================================
-- Bảng Match Players (Người chơi trong trận)
-- ============================================
CREATE TABLE match_players (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    user_id INT NOT NULL,
    role ENUM('VILLAGER', 'WEREWOLF', 'SEER') NOT NULL,
    is_alive BOOLEAN DEFAULT TRUE,
    died_at_round INT NULL COMMENT 'Round nào chết (nếu chết)',
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_match (match_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Bảng Chat Messages (Optional - MVP có thể bỏ qua)
-- ============================================
CREATE TABLE chat_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NULL COMMENT 'NULL nếu là chat trong match',
    match_id INT NULL COMMENT 'NULL nếu là chat trong room',
    user_id INT NULL COMMENT 'NULL nếu là system message',
    message_type ENUM('PUBLIC', 'WEREWOLF', 'SYSTEM') DEFAULT 'PUBLIC',
    content TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_room (room_id),
    INDEX idx_match (match_id),
    INDEX idx_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Bảng Event Logs (Cho replay)
-- ============================================
CREATE TABLE event_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    round_number INT NOT NULL,
    game_phase ENUM('WAITING', 'STARTING', 'NIGHT_WOLF', 'NIGHT_SEER', 'DAY_ANNOUNCE', 'DAY_CHAT', 'DAY_VOTE', 'ENDED') NOT NULL,
    event_type VARCHAR(50) NOT NULL COMMENT 'Ví dụ: KILL, VOTE, SEER_CHECK, etc.',
    actor_user_id INT NULL COMMENT 'Người thực hiện hành động',
    target_user_id INT NULL COMMENT 'Người bị tác động',
    event_data JSON NULL COMMENT 'Dữ liệu bổ sung dạng JSON',
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_match_round (match_id, round_number),
    INDEX idx_phase (game_phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Sample Data (Optional - cho testing)
-- ============================================
-- INSERT INTO users (username, password_hash, display_name) VALUES
-- ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin'),
-- ('player1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Player 1');
