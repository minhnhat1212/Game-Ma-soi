package com.werewolf.shared.dto;

import com.werewolf.shared.enums.Role;

public class PlayerDTO {
    private int userId;
    private String displayName;
    private boolean ready;
    private Role role; // NULL nếu game chưa start
    private boolean alive; // NULL nếu game chưa start
    private boolean isHost;
    private boolean silenced; // Đang bị câm chat không

    public PlayerDTO() {
    }

    public PlayerDTO(int userId, String displayName, boolean ready, Role role, boolean alive, boolean isHost) {
        this.userId = userId;
        this.displayName = displayName;
        this.ready = ready;
        this.role = role;
        this.alive = alive;
        this.isHost = isHost;
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

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public boolean isSilenced() {
        return silenced;
    }

    public void setSilenced(boolean silenced) {
        this.silenced = silenced;
    }
}
