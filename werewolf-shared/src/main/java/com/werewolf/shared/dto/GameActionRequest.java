package com.werewolf.shared.dto;

/**
 * Request cho các hành động trong game: kill, vote, seer check
 */
public class GameActionRequest extends Message {
    private String action; // "KILL", "VOTE", "SEER_CHECK"
    private Integer targetUserId; // ID người chơi mục tiêu

    public GameActionRequest() {
        this.type = "GAME_ACTION_REQUEST";
    }

    public GameActionRequest(String action, Integer targetUserId) {
        this();
        this.action = action;
        this.targetUserId = targetUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }
}
