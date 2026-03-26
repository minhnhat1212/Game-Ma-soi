package com.werewolf.shared.enums;

/**
 * Loại tin nhắn chat
 */
public enum MessageType {
    PUBLIC("Công khai"),
    WEREWOLF("Sói"),
    SYSTEM("Hệ thống");

    private final String displayName;

    MessageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
