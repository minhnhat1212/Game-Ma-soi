package com.werewolf.shared.dto;

public class RoleConfig {
    private int werewolfCount;
    private int villagerCount;
    private boolean hasSeer;
    private boolean hasGuard;
    private boolean hasWitch;
    private boolean hasHunter;

    public RoleConfig() {
        // Default Configuration
        this.werewolfCount = 1;
        this.villagerCount = 0; // Calculated dynamically if 0
        this.hasSeer = true;
        this.hasGuard = false;
        this.hasWitch = false;
        this.hasHunter = false;
    }

    public RoleConfig(int werewolfCount, boolean hasSeer, boolean hasGuard, boolean hasWitch, boolean hasHunter) {
        this.werewolfCount = werewolfCount;
        this.hasSeer = hasSeer;
        this.hasGuard = hasGuard;
        this.hasWitch = hasWitch;
        this.hasHunter = hasHunter;
    }

    public int getWerewolfCount() {
        return werewolfCount;
    }

    public void setWerewolfCount(int werewolfCount) {
        this.werewolfCount = werewolfCount;
    }

    public int getVillagerCount() {
        return villagerCount;
    }

    public void setVillagerCount(int villagerCount) {
        this.villagerCount = villagerCount;
    }

    public boolean isHasSeer() {
        return hasSeer;
    }

    public void setHasSeer(boolean hasSeer) {
        this.hasSeer = hasSeer;
    }

    public boolean isHasGuard() {
        return hasGuard;
    }

    public void setHasGuard(boolean hasGuard) {
        this.hasGuard = hasGuard;
    }

    public boolean isHasWitch() {
        return hasWitch;
    }

    public void setHasWitch(boolean hasWitch) {
        this.hasWitch = hasWitch;
    }

    public boolean isHasHunter() {
        return hasHunter;
    }

    public void setHasHunter(boolean hasHunter) {
        this.hasHunter = hasHunter;
    }
}
