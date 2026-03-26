package com.werewolf.shared.enums;

/**
 * Vai trò trong game Ma Sói
 */
public enum Role {
    VILLAGER("Dân làng"),
    WEREWOLF("Ma Sói"),
    SEER("Tiên Tri"),
    GUARD("Bảo vệ"),
    WITCH("Phù thủy"),
    HUNTER("Thợ săn");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Kiểm tra vai trò có phải sói không
     */
    public boolean isWerewolf() {
        return this == WEREWOLF;
    }

    /**
     * Kiểm tra vai trò có phải dân không
     */
    public boolean isVillager() {
        return this == VILLAGER || this == SEER;
    }
}
