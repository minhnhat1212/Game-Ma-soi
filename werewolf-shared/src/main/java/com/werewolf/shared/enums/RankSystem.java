package com.werewolf.shared.enums;

/**
 * Hệ thống hạng (Rank) dựa theo Level người chơi
 */
public enum RankSystem {
    TAN_BINH(1, 4, "Tân Binh", "🌱", "#78c850"),
    DAN_LANG(5, 9, "Dân Làng", "🏡", "#a0c878"),
    THAM_TU(10, 14, "Thám Tử", "🔍", "#6890f0"),
    CHIEN_BINH(15, 19, "Chiến Binh", "⚔️", "#f08030"),
    PHU_THUY(20, 29, "Phù Thủy", "🔮", "#a040a0"),
    LANH_CHUA(30, 49, "Lãnh Chúa", "👑", "#f8d030"),
    HUYEN_THOAI(50, Integer.MAX_VALUE, "Huyền Thoại", "🌟", "#ff4444");

    private final int minLevel;
    private final int maxLevel;
    private final String displayName;
    private final String icon;
    private final String color;

    RankSystem(int minLevel, int maxLevel, String displayName, String icon, String color) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }

    /**
     * Lấy rank từ level
     */
    public static RankSystem fromLevel(int level) {
        for (RankSystem rank : values()) {
            if (level >= rank.minLevel && level <= rank.maxLevel) {
                return rank;
            }
        }
        return TAN_BINH;
    }

    /**
     * Lấy tên rank đầy đủ (icon + tên)
     */
    public String getFullDisplayName() {
        return icon + " " + displayName;
    }

    /**
     * XP cần để đạt level tiếp theo
     * Công thức: level * 100 XP
     */
    public static int xpRequiredForLevel(int level) {
        return level * 100;
    }

    /**
     * Tính level từ tổng XP
     */
    public static int levelFromTotalXp(int totalXp) {
        int level = 1;
        int xpUsed = 0;
        while (true) {
            int xpForNext = xpRequiredForLevel(level);
            if (xpUsed + xpForNext > totalXp) {
                break;
            }
            xpUsed += xpForNext;
            level++;
        }
        return level;
    }

    /**
     * XP còn lại trong level hiện tại (để hiển thị progress bar)
     */
    public static int xpInCurrentLevel(int totalXp) {
        int level = 1;
        int xpUsed = 0;
        while (true) {
            int xpForNext = xpRequiredForLevel(level);
            if (xpUsed + xpForNext > totalXp) {
                return totalXp - xpUsed;
            }
            xpUsed += xpForNext;
            level++;
        }
    }
}
