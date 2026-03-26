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
}
