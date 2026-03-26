package com.werewolf.server.service;

import com.werewolf.server.entity.PlayerState;
import com.werewolf.shared.enums.Role;

import java.util.*;

/**
 * AI logic cho bot
 */
public class BotAI {
    private static final Random random = new Random();
    
    /**
     * Bot chọn mục tiêu dựa trên vai trò và tình huống
     */
    public Integer chooseTarget(Role role, PlayerState bot, List<PlayerState> allPlayers, 
                               List<PlayerState> validTargets, GameEngine gameEngine) {
        if (validTargets.isEmpty()) {
            return null;
        }
        
        switch (role) {
            case WEREWOLF:
                return chooseWerewolfTarget(bot, allPlayers, validTargets);
            case SEER:
                return chooseSeerTarget(bot, allPlayers, validTargets);
            case VILLAGER:
                return chooseVillagerVote(bot, allPlayers, validTargets);
            default:
                // Random nếu không biết vai trò
                return validTargets.get(random.nextInt(validTargets.size())).getUserId();
        }
    }
    
    /**
     * Sói chọn mục tiêu: ưu tiên giết người chơi có khả năng là tiên tri hoặc dân làng quan trọng
     */
    private Integer chooseWerewolfTarget(PlayerState bot, List<PlayerState> allPlayers, 
                                        List<PlayerState> validTargets) {
        // Strategy: Giết người chơi có nhiều khả năng là tiên tri hoặc dân làng tích cực
        // Tạm thời random, có thể cải thiện logic sau
        return validTargets.get(random.nextInt(validTargets.size())).getUserId();
    }
    
    /**
     * Tiên tri chọn mục tiêu: ưu tiên soi người chơi đáng nghi
     */
    private Integer chooseSeerTarget(PlayerState bot, List<PlayerState> allPlayers, 
                                     List<PlayerState> validTargets) {
        // Strategy: Soi người chơi có hành vi đáng nghi hoặc random
        // Tạm thời random, có thể cải thiện logic sau
        return validTargets.get(random.nextInt(validTargets.size())).getUserId();
    }
    
    /**
     * Dân làng vote: ưu tiên vote người đáng nghi
     */
    private Integer chooseVillagerVote(PlayerState bot, List<PlayerState> allPlayers, 
                                       List<PlayerState> validTargets) {
        // Strategy: Vote người chơi có hành vi đáng nghi hoặc random
        // Tạm thời random, có thể cải thiện logic sau
        return validTargets.get(random.nextInt(validTargets.size())).getUserId();
    }
}