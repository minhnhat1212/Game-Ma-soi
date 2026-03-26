package com.werewolf.shared.dto;

import java.util.List;

public class RoomDTO {
    private int id;
    private String name;
    private int hostUserId;
    private String hostDisplayName;
    private int maxPlayers;
    private int currentPlayers;
    private int phaseDurationSeconds;
    private String status; // WAITING, PLAYING, ENDED
    private boolean hasPassword;
    private List<PlayerDTO> players;

    public RoomDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(int hostUserId) {
        this.hostUserId = hostUserId;
    }

    public String getHostDisplayName() {
        return hostDisplayName;
    }

    public void setHostDisplayName(String hostDisplayName) {
        this.hostDisplayName = hostDisplayName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public int getPhaseDurationSeconds() {
        return phaseDurationSeconds;
    }

    public void setPhaseDurationSeconds(int phaseDurationSeconds) {
        this.phaseDurationSeconds = phaseDurationSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    private RoleConfig roleConfig;

    public void setRoleConfig(RoleConfig roleConfig) {
        this.roleConfig = roleConfig;
    }

    public RoleConfig getRoleConfig() {
        return roleConfig;
    }
}
