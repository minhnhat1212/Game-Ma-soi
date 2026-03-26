-- ============================================
-- Script đơn giản để fix max_players constraint
-- Cho phép từ 4-16 người chơi (thay vì 8-16)
-- ============================================

USE werewolf_game;

-- Cách đơn giản nhất: Sửa trực tiếp constraint nếu MySQL 8.0+
-- Nếu lỗi, có thể cần xóa và tạo lại constraint

-- Tìm tên constraint hiện tại
SELECT CONSTRAINT_NAME 
FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS 
WHERE TABLE_SCHEMA = 'werewolf_game' 
  AND TABLE_NAME = 'rooms'
  AND CHECK_CLAUSE LIKE '%max_players%';

-- Sau khi biết tên constraint, chạy lệnh sau (thay rooms_chk_1 bằng tên thực tế):
-- ALTER TABLE rooms DROP CHECK rooms_chk_1;
-- ALTER TABLE rooms ADD CONSTRAINT rooms_max_players_check CHECK (max_players BETWEEN 4 AND 16);

-- Hoặc nếu MySQL không hỗ trợ DROP CHECK, dùng cách này:
-- 1. Xóa constraint bằng cách modify column (MySQL sẽ tự động xóa constraint cũ)
ALTER TABLE rooms 
MODIFY COLUMN max_players INT NOT NULL DEFAULT 8;

-- 2. Thêm constraint mới
ALTER TABLE rooms 
ADD CONSTRAINT rooms_max_players_check 
CHECK (max_players BETWEEN 4 AND 16);

-- Kiểm tra kết quả
SELECT 
    CONSTRAINT_NAME,
    CHECK_CLAUSE
FROM 
    INFORMATION_SCHEMA.CHECK_CONSTRAINTS
WHERE 
    TABLE_SCHEMA = 'werewolf_game' 
    AND TABLE_NAME = 'rooms';
