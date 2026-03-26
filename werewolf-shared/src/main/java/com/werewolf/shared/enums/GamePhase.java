package com.werewolf.shared.enums;

/**
 * Các pha trong game
 */
public enum GamePhase {
    WAITING("Chờ người chơi"),
    STARTING("Bắt đầu game"),
    NIGHT_WOLF("Đêm - Ma Sói"),
    NIGHT_GUARD("Đêm - Bảo vệ"),
    NIGHT_WITCH("Đêm - Phù thủy"),
    NIGHT_SEER("Đêm - Tiên Tri"),
    DAY_ANNOUNCE("Ngày - Công bố"),
    DAY_HUNTER("Ngày - Thợ Săn báo thù"),
    DAY_CHAT("Ngày - Thảo luận"),
    DAY_VOTE("Ngày - Bỏ phiếu"),
    ENDED("Kết thúc");

    private final String displayName;

    GamePhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
