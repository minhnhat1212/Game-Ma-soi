package com.werewolf.shared.enums;

/**
 * Mã lỗi từ server
 */
public enum ErrorCode {
    // Auth errors (1000-1099)
    INVALID_CREDENTIALS(1001, "Sai tên đăng nhập hoặc mật khẩu"),
    USERNAME_EXISTS(1002, "Tên đăng nhập đã tồn tại"),
    USER_NOT_FOUND(1003, "Không tìm thấy người dùng"),
    ALREADY_LOGGED_IN(1004, "Đã đăng nhập rồi"),

    // Room errors (1100-1199)
    ROOM_NOT_FOUND(1101, "Không tìm thấy phòng"),
    ROOM_FULL(1102, "Phòng đã đầy"),
    ROOM_PASSWORD_INCORRECT(1103, "Sai mật khẩu phòng"),
    ROOM_ALREADY_STARTED(1104, "Phòng đã bắt đầu game"),
    NOT_HOST(1105, "Bạn không phải host"),
    ALREADY_IN_ROOM(1106, "Bạn đã ở trong phòng"),
    NOT_IN_ROOM(1107, "Bạn không ở trong phòng"),

    // Game errors (1200-1299)
    GAME_NOT_STARTED(1201, "Game chưa bắt đầu"),
    INVALID_PHASE(1202, "Không đúng pha game"),
    INVALID_ACTION(1203, "Hành động không hợp lệ"),
    NOT_YOUR_TURN(1204, "Chưa đến lượt bạn"),
    TARGET_NOT_FOUND(1205, "Không tìm thấy mục tiêu"),
    ALREADY_VOTED(1206, "Bạn đã vote rồi"),
    INSUFFICIENT_PLAYERS(1207, "Chưa đủ người chơi"),

    // General errors (1300-1399)
    INVALID_REQUEST(1301, "Yêu cầu không hợp lệ"),
    SERVER_ERROR(1302, "Lỗi server"),
    UNAUTHORIZED(1303, "Chưa đăng nhập");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
