package com.werewolf.shared.dto;

import com.werewolf.shared.enums.GamePhase;
import java.util.List;
import java.util.Map;

public class GameStateUpdate extends Message {
    private GamePhase phase;
    private int roundNumber;
    private int timeRemaining; // seconds
    private List<PlayerDTO> players;
    private Map<Integer, Integer> votes; // userId -> vote count
    private Integer killedPlayerId; // Người bị giết đêm qua
    private String winnerTeam; // "VILLAGERS" hoặc "WEREWOLVES"
    private boolean canAct; // Client có thể thực hiện hành động không
    private boolean hunterMustAct; // Hunter phải chọn người kéo theo

    // === Kỹ năng đặc biệt ===
    // Bảo vệ
    private boolean guardCanSelfProtect;   // Còn quyền tự bảo vệ không
    private boolean guardSelfProtectUsed;  // Đã dùng tự bảo vệ chưa
    // Tiên tri
    private boolean seerHasIntuition;      // Còn quyền trực giác không
    private boolean seerIntuitionUsed;     // Đã dùng trực giác chưa
    // Ma sói
    private boolean werewolfCanFrame;      // Có thể frame người không
    private boolean werewolfFrameUsed;     // Đã frame chưa
    // Phù thủy
    private boolean witchHasMiniRevive;    // Còn quyền hồi sinh tạm không
    private boolean witchMiniReviveUsed;   // Đã dùng chưa

    // === Phần thưởng sau ván ===
    private int xpGained;     // XP nhận được
    private int coinsGained;  // Coin nhận được
    private int bonusXp;      // Bonus XP (sống sót, vai trò đặc biệt)
    private String newRank;   // Rank mới sau ván

    public GameStateUpdate() {
        this.type = "GAME_STATE_UPDATE";
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(int timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    public Map<Integer, Integer> getVotes() {
        return votes;
    }

    public void setVotes(Map<Integer, Integer> votes) {
        this.votes = votes;
    }

    public Integer getKilledPlayerId() {
        return killedPlayerId;
    }

    public void setKilledPlayerId(Integer killedPlayerId) {
        this.killedPlayerId = killedPlayerId;
    }

    public String getWinnerTeam() {
        return winnerTeam;
    }

    public void setWinnerTeam(String winnerTeam) {
        this.winnerTeam = winnerTeam;
    }

    public boolean isCanAct() {
        return canAct;
    }

    public void setCanAct(boolean canAct) {
        this.canAct = canAct;
    }

    public boolean isHunterMustAct() {
        return hunterMustAct;
    }

    public void setHunterMustAct(boolean hunterMustAct) {
        this.hunterMustAct = hunterMustAct;
    }

    // === Getters/Setters kỹ năng ===

    public boolean isGuardCanSelfProtect() { return guardCanSelfProtect; }
    public void setGuardCanSelfProtect(boolean v) { this.guardCanSelfProtect = v; }

    public boolean isGuardSelfProtectUsed() { return guardSelfProtectUsed; }
    public void setGuardSelfProtectUsed(boolean v) { this.guardSelfProtectUsed = v; }

    public boolean isSeerHasIntuition() { return seerHasIntuition; }
    public void setSeerHasIntuition(boolean v) { this.seerHasIntuition = v; }

    public boolean isSeerIntuitionUsed() { return seerIntuitionUsed; }
    public void setSeerIntuitionUsed(boolean v) { this.seerIntuitionUsed = v; }

    public boolean isWerewolfCanFrame() { return werewolfCanFrame; }
    public void setWerewolfCanFrame(boolean v) { this.werewolfCanFrame = v; }

    public boolean isWerewolfFrameUsed() { return werewolfFrameUsed; }
    public void setWerewolfFrameUsed(boolean v) { this.werewolfFrameUsed = v; }

    public boolean isWitchHasMiniRevive() { return witchHasMiniRevive; }
    public void setWitchHasMiniRevive(boolean v) { this.witchHasMiniRevive = v; }

    public boolean isWitchMiniReviveUsed() { return witchMiniReviveUsed; }
    public void setWitchMiniReviveUsed(boolean v) { this.witchMiniReviveUsed = v; }

    // === Phần thưởng ===

    public int getXpGained() { return xpGained; }
    public void setXpGained(int xpGained) { this.xpGained = xpGained; }

    public int getCoinsGained() { return coinsGained; }
    public void setCoinsGained(int coinsGained) { this.coinsGained = coinsGained; }

    public int getBonusXp() { return bonusXp; }
    public void setBonusXp(int bonusXp) { this.bonusXp = bonusXp; }

    public String getNewRank() { return newRank; }
    public void setNewRank(String newRank) { this.newRank = newRank; }
}
