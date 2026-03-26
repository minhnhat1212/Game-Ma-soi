-- ============================================
-- Fix max_players constraint để cho phép từ 4-16 người chơi
-- (thay vì 8-16) để hỗ trợ chơi với bot
-- ============================================

USE werewolf_game;

-- Lưu ý: MySQL 8.0+ mới hỗ trợ CHECK constraint đầy đủ
-- Nếu dùng MySQL < 8.0, có thể bỏ qua constraint này và validate trong code

-- Xóa constraint cũ (nếu có tên cụ thể)
-- Kiểm tra tên constraint trước:
-- SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS 
-- WHERE TABLE_SCHEMA = 'werewolf_game' AND TABLE_NAME = 'rooms';

-- Cách 1: Nếu MySQL 8.0+, dùng ALTER TABLE để sửa constraint
-- ALTER TABLE rooms DROP CHECK rooms_chk_1;  -- Thay rooms_chk_1 bằng tên constraint thực tế
-- ALTER TABLE rooms ADD CONSTRAINT rooms_max_players_check CHECK (max_players BETWEEN 4 AND 16);

-- Cách 2: Đơn giản nhất - Xóa và tạo lại bảng (CHỈ DÙNG NẾU CHƯA CÓ DỮ LIỆU QUAN TRỌNG!)
-- Hoặc dùng cách này nếu muốn giữ nguyên dữ liệu:

-- Bước 1: Tạo bảng tạm với constraint mới
CREATE TABLE rooms_new LIKE rooms;
ALTER TABLE rooms_new DROP CHECK IF EXISTS rooms_chk_1;
ALTER TABLE rooms_new ADD CONSTRAINT rooms_max_players_check CHECK (max_players BETWEEN 4 AND 16);

-- Bước 2: Copy dữ liệu
INSERT INTO rooms_new SELECT * FROM rooms;

-- Bước 3: Xóa bảng cũ và đổi tên
DROP TABLE rooms;
RENAME TABLE rooms_new TO rooms;

-- Bước 4: Khôi phục foreign keys và indexes
ALTER TABLE rooms 
    ADD CONSTRAINT rooms_host_fk FOREIGN KEY (host_user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD INDEX idx_status (status),
    ADD INDEX idx_host (host_user_id);

ALTER TABLE room_members 
    ADD CONSTRAINT room_members_room_fk FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE;

ALTER TABLE matches 
    ADD CONSTRAINT matches_room_fk FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL;

ALTER TABLE chat_messages 
    ADD CONSTRAINT chat_room_fk FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE;

-- Kiểm tra constraint đã được áp dụng
SELECT 
    CONSTRAINT_NAME,
    CHECK_CLAUSE
FROM 
    INFORMATION_SCHEMA.CHECK_CONSTRAINTS
WHERE 
    TABLE_SCHEMA = 'werewolf_game' 
    AND TABLE_NAME = 'rooms';
