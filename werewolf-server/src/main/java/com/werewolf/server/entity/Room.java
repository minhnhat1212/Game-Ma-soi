package com.werewolf.server.entity;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Entity đại diện cho Room trong memory (có thể sync với DB)
 */
public class Room {
    private int id;
    private String name;
    private String passwordHash; // null nếu public
    private int hostUserId;
    private int maxPlayers;
    private int phaseDurationSeconds;
    private String status; // WAITING, PLAYING, ENDED
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    
    // Configuration
    private com.werewolf.shared.dto.RoleConfig roleConfig;

    // In-memory data
    private ConcurrentMap<Integer, PlayerState> players = new ConcurrentHashMap<>(); // userId -> PlayerState

    public Room() {
    }

    public Room(int id, String name, String passwordHash, int hostUserId, 
                int maxPlayers, int phaseDurationSeconds, String status) {
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
        this.hostUserId = hostUserId;
        this.maxPlayers = maxPlayers;
        this.phaseDurationSeconds = phaseDurationSeconds;
        this.status = status;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(int hostUserId) {
        this.hostUserId = hostUserId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public ConcurrentMap<Integer, PlayerState> getPlayers() {
        return players;
    }

    public int getCurrentPlayers() {
        return players.size();
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    public com.werewolf.shared.dto.RoleConfig getRoleConfig() {
        return roleConfig;
    }

    public void setRoleConfig(com.werewolf.shared.dto.RoleConfig roleConfig) {
        this.roleConfig = roleConfig;
    }
}
