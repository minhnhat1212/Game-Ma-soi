package com.werewolf.server.service;

import com.werewolf.server.entity.PlayerState;
import com.werewolf.server.repository.UserRepository;
import com.werewolf.shared.enums.RankSystem;
import com.werewolf.shared.enums.Role;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ProgressionService — Tính toán phần thưởng, XP, Rank sau mỗi ván đấu
 *
 * Quy tắc thưởng:
 *  - Tham gia ván:       20 XP + 10 Coin (thua)
 *  - Thắng ván:          50 XP + 30 Coin
 *  - Sống sót đến cuối:  +10 XP bonus + 5 Coin bonus
 *  - Vai trò đặc biệt (SEER/GUARD/WITCH) thắng: +10 XP bonus
 */
public class ProgressionService {

    private static final int BASE_WIN_XP    = 50;
    private static final int BASE_LOSE_XP   = 20;
    private static final int BASE_WIN_COIN  = 30;
    private static final int BASE_LOSE_COIN = 10;

    private static final int BONUS_SURVIVE_XP   = 10;
    private static final int BONUS_SURVIVE_COIN =  5;
    private static final int BONUS_ROLE_XP      = 10; // Vai trò đặc biệt thắng

    private final UserRepository userRepository;

    public ProgressionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tính và áp dụng phần thưởng sau ván đấu.
     *
     * @param winnerUserIds     Set userId thắng
     * @param participantStates Map userId -> PlayerState của người thật (không phải bot)
     * @return Map userId -> RewardResult (để trả về client)
     */
    public Map<Integer, RewardResult> applyRewards(
            Set<Integer> winnerUserIds,
            Map<Integer, PlayerState> participantStates) {

        Map<Integer, RewardResult> results = new HashMap<>();

        for (Map.Entry<Integer, PlayerState> entry : participantStates.entrySet()) {
            int userId = entry.getKey();
            PlayerState ps = entry.getValue();

            boolean isWinner = winnerUserIds.contains(userId);
            boolean survived = ps.isAlive();
            Role role = ps.getRole();

            int xp = isWinner ? BASE_WIN_XP : BASE_LOSE_XP;
            int coin = isWinner ? BASE_WIN_COIN : BASE_LOSE_COIN;
            int bonusXp = 0;

            // Bonus sống sót
            if (survived) {
                bonusXp += BONUS_SURVIVE_XP;
                coin += BONUS_SURVIVE_COIN;
            }

            // Bonus vai trò đặc biệt thắng
            if (isWinner && role != null && (role == Role.SEER || role == Role.GUARD || role == Role.WITCH)) {
                bonusXp += BONUS_ROLE_XP;
            }

            int totalXp = xp + bonusXp;

            RewardResult result = new RewardResult(userId, xp, bonusXp, coin, isWinner, survived);
            results.put(userId, result);
        }

        // Áp dụng vào DB
        userRepository.applyMatchRewardsDetailed(results);

        return results;
    }

    /**
     * Tính rank name từ level
     */
    public static String getRankName(int level) {
        return RankSystem.fromLevel(level).getFullDisplayName();
    }

    /**
     * Kết quả thưởng của 1 người chơi
     */
    public static class RewardResult {
        public final int userId;
        public final int baseXp;
        public final int bonusXp;
        public final int coinsGained;
        public final boolean won;
        public final boolean survived;

        public RewardResult(int userId, int baseXp, int bonusXp, int coinsGained, boolean won, boolean survived) {
            this.userId = userId;
            this.baseXp = baseXp;
            this.bonusXp = bonusXp;
            this.coinsGained = coinsGained;
            this.won = won;
            this.survived = survived;
        }

        public int getTotalXp() {
            return baseXp + bonusXp;
        }
    }
}
