package com.werewolf.shared.dto;

public class CreateRoomRequest extends Message {
    private String roomName;
    private String password; // Optional, null nếu public
    private int maxPlayers;
    private int phaseDurationSeconds;

    public CreateRoomRequest() {
        this.type = "CREATE_ROOM_REQUEST";
    }

    public CreateRoomRequest(String roomName, String password, int maxPlayers, int phaseDurationSeconds) {
        this();
        this.roomName = roomName;
        this.password = password;
        this.maxPlayers = maxPlayers;
        this.phaseDurationSeconds = phaseDurationSeconds;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getPhaseDurationSeconds() {
        return phaseDurationSeconds;
    }

    public void setPhaseDurationSeconds(int phaseDurationSeconds) {
        this.phaseDurationSeconds = phaseDurationSeconds;
    }

    private RoleConfig roleConfig;

    public void setRoleConfig(RoleConfig roleConfig) {
        this.roleConfig = roleConfig;
    }

    public RoleConfig getRoleConfig() {
        return roleConfig;
    }
}
