package com.werewolf.server.service;

import com.werewolf.server.entity.PlayerState;
import com.werewolf.shared.enums.Role;

import java.util.List;

/**
 * Bot player - tự động chơi game
 */
public class BotPlayer {
    private static final int BOT_ID_BASE = -1000; // Bot IDs bắt đầu từ -1000
    private static int botCounter = 0;
    
    private final int botId;
    private final String displayName;
    private final PlayerState playerState;
    private final BotAI botAI;
    
    public BotPlayer(int botId, String displayName) {
        this.botId = botId;
        this.displayName = displayName;
        this.playerState = new PlayerState(botId, displayName);
        this.botAI = new BotAI();
    }
    
    /**
     * Tạo bot mới với ID và tên tự động
     */
    public static BotPlayer createBot() {
        int id = BOT_ID_BASE - botCounter++;
        String name = "Bot " + (botCounter);
        return new BotPlayer(id, name);
    }
    
    public int getBotId() {
        return botId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public PlayerState getPlayerState() {
        return playerState;
    }
    
    /**
     * Bot tự động ready
     */
    public void autoReady() {
        playerState.setReady(true);
    }
    
    /**
     * Bot chọn hành động dựa trên pha game và vai trò
     */
    public Integer chooseAction(GameEngine gameEngine, List<PlayerState> allPlayers) {
        Role role = playerState.getRole();
        if (role == null || !playerState.isAlive()) {
            return null;
        }
        
        // Lấy danh sách mục tiêu hợp lệ
        List<PlayerState> validTargets = getValidTargets(allPlayers);
        if (validTargets.isEmpty()) {
            return null;
        }
        
        return botAI.chooseTarget(role, playerState, allPlayers, validTargets, gameEngine);
    }
    
    /**
     * Lấy danh sách mục tiêu hợp lệ dựa trên vai trò
     */
    private List<PlayerState> getValidTargets(List<PlayerState> allPlayers) {
        return allPlayers.stream()
            .filter(p -> p.getUserId() != botId && p.isAlive())
            .filter(p -> {
                // Sói không thể giết sói khác
                if (playerState.getRole() == Role.WEREWOLF) {
                    return p.getRole() != Role.WEREWOLF;
                }
                return true;
            })
            .toList();
    }
}