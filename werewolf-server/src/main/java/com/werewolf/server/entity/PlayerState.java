package com.werewolf.server.entity;

import com.werewolf.shared.enums.Role;

/**
 * Trạng thái của một người chơi trong room/game
 */
public class PlayerState {
    private int userId;
    private String displayName;
    private boolean ready;
    private Role role; // null nếu game chưa start
    private boolean alive; // true nếu còn sống
    private boolean voted; // đã vote chưa (trong pha vote)
    private Integer voteTarget; // userId của người được vote
    
    // Witch potions
    private boolean hasSavePotion = true;
    private boolean hasKillPotion = true;

    // Kỹ năng đặc biệt
    private boolean guardSelfProtectUsed = false;  // Bảo vệ đã tự bảo vệ chưa
    private boolean seerIntuitionUsed = false;     // Tiên tri đã dùng trực giác chưa
    private boolean witchMiniReviveUsed = false;   // Phù thủy đã hồi sinh tạm chưa
    private boolean werewolfFrameUsed = false;     // Sói đã frame người chưa

    public PlayerState() {
    }

    public PlayerState(int userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.ready = false;
        this.alive = true;
        this.voted = false;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isVoted() {
        return voted;
    }

    public void setVoted(boolean voted) {
        this.voted = voted;
    }

    public Integer getVoteTarget() {
        return voteTarget;
    }

    public void setVoteTarget(Integer voteTarget) {
        this.voteTarget = voteTarget;
    }

    public boolean isHasSavePotion() {
        return hasSavePotion;
    }

    public void setHasSavePotion(boolean hasSavePotion) {
        this.hasSavePotion = hasSavePotion;
    }

    public boolean isHasKillPotion() {
        return hasKillPotion;
    }

    public void setHasKillPotion(boolean hasKillPotion) {
        this.hasKillPotion = hasKillPotion;
    }

    public boolean isGuardSelfProtectUsed() { return guardSelfProtectUsed; }
    public void setGuardSelfProtectUsed(boolean v) { this.guardSelfProtectUsed = v; }

    public boolean isSeerIntuitionUsed() { return seerIntuitionUsed; }
    public void setSeerIntuitionUsed(boolean v) { this.seerIntuitionUsed = v; }

    public boolean isWitchMiniReviveUsed() { return witchMiniReviveUsed; }
    public void setWitchMiniReviveUsed(boolean v) { this.witchMiniReviveUsed = v; }

    public boolean isWerewolfFrameUsed() { return werewolfFrameUsed; }
    public void setWerewolfFrameUsed(boolean v) { this.werewolfFrameUsed = v; }
}
