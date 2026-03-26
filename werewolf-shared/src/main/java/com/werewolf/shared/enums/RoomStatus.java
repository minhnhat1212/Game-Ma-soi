package com.werewolf.shared.enums;

/**
 * Trạng thái phòng
 */
public enum RoomStatus {
    WAITING("Chờ người chơi"),
    PLAYING("Đang chơi"),
    ENDED("Kết thúc");

    private final String displayName;

    RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
