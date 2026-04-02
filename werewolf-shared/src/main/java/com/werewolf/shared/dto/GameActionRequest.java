package com.werewolf.shared.dto;

/**
 * Request cho các hành động trong game: kill, vote, seer check, kỹ năng đặc biệt
 */
public class GameActionRequest extends Message {
    private String action;
    private Integer targetUserId;   // Mục tiêu chính
    private Integer target2UserId;  // Mục tiêu phụ (dùng cho SEER_INTUITION)

    public GameActionRequest() {
        this.type = "GAME_ACTION_REQUEST";
    }

    public GameActionRequest(String action, Integer targetUserId) {
        this();
        this.action = action;
        this.targetUserId = targetUserId;
    }

    public GameActionRequest(String action, Integer targetUserId, Integer target2UserId) {
        this();
        this.action = action;
        this.targetUserId = targetUserId;
        this.target2UserId = target2UserId;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Integer getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Integer targetUserId) { this.targetUserId = targetUserId; }

    public Integer getTarget2UserId() { return target2UserId; }
    public void setTarget2UserId(Integer target2UserId) { this.target2UserId = target2UserId; }
}
