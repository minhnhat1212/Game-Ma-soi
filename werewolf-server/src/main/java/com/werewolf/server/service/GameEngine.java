package com.werewolf.server.service;

import com.werewolf.server.entity.PlayerState;
import com.werewolf.server.entity.Room;
import com.werewolf.server.repository.UserRepository;
import com.werewolf.shared.dto.GameStateUpdate;
import com.werewolf.shared.dto.PlayerDTO;
import com.werewolf.shared.enums.GamePhase;
import com.werewolf.shared.enums.Role;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Game Engine - Quản lý logic game và state machine
 * Mỗi room có một GameEngine instance riêng
 */
public class GameEngine {
    private final Room room;
    private final RoomService roomService;
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final UserRepository userRepository = new UserRepository();

    private GamePhase currentPhase = GamePhase.WAITING;
    private int roundNumber = 0;
    private int timeRemaining = 0;
    private Integer killedPlayerId = null; // Người bị giết đêm qua (hoặc bị treo cổ)
    private Map<Integer, Integer> votes = new ConcurrentHashMap<>(); // userId -> vote count
    private Map<Integer, Integer> playerVotes = new ConcurrentHashMap<>(); // userId -> targetUserId (ai vote ai)
    private String winnerTeam = null;

    // Night actions
    private Map<Integer, Integer> werewolfKills = new ConcurrentHashMap<>(); // userId -> targetUserId
    private Map<Integer, Integer> seerChecks = new ConcurrentHashMap<>();   // userId -> targetUserId
    // New Roles
    private Map<Integer, Integer> guardProtects = new ConcurrentHashMap<>(); // userId -> targetUserId
    private boolean witchSaved = false;
    private Integer witchKilledId = null;
    private Map<Integer, Integer> silencedUntilRound = new ConcurrentHashMap<>();

    // Hunter
    private boolean hunterPendingAction = false; // Hunter bị sói giết, chờ báo thù
    private Integer hunterId = null;             // ID của Hunter đang chờ báo thù

    private boolean matchRewardsApplied = false;

    public GameEngine(Room room, RoomService roomService) {
        this.room = room;
        this.roomService = roomService;
    }

    /**
     * Bắt đầu game
     */
    public void startGame() {
        lock.lock();
        try {
            if (currentPhase != GamePhase.WAITING) {
                throw new RuntimeException("Game đã bắt đầu");
            }

            matchRewardsApplied = false;

            // Tự động thêm bot nếu thiếu người chơi (tối thiểu 4 người)
            int minPlayers = 4;
            int neededBots = Math.max(0, minPlayers - room.getCurrentPlayers());
            if (neededBots > 0) {
                roomService.addBotsToRoom(room, neededBots);
            }

            // Kiểm tra tất cả đã ready (bao gồm cả bot)
            for (PlayerState player : room.getPlayers().values()) {
                if (!player.isReady()) {
                    throw new RuntimeException("Chưa tất cả người chơi đã ready");
                }
            }

            if (room.getCurrentPlayers() < minPlayers) {
                throw new RuntimeException("Cần tối thiểu " + minPlayers + " người chơi");
            }

            // Chia vai trò
            roomService.assignRoles(room);

            // Chuyển sang pha STARTING
            currentPhase = GamePhase.STARTING;
            timeRemaining = 3;
            room.setStatus("PLAYING");
            roomService.updateRoomStatus(room.getId(), "PLAYING");

            // Sau 3 giây chuyển sang đêm
            scheduler.schedule(() -> {
                startNight();
            }, 3, TimeUnit.SECONDS);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Bắt đầu đêm
     */
    private void startNight() {
        lock.lock();
        try {
            roundNumber++;
            currentPhase = GamePhase.NIGHT_WOLF;
            killedPlayerId = null;
            votes.clear();
            playerVotes.clear();
            werewolfKills.clear();
            seerChecks.clear();
            // Clear new role actions
            guardProtects.clear();
            witchSaved = false;
            witchKilledId = null;
            // Reset hunter state
            hunterPendingAction = false;
            hunterId = null;

            // Reset vote status
            for (PlayerState player : room.getPlayers().values()) {
                player.setVoted(false);
                player.setVoteTarget(null);
            }

            timeRemaining = room.getPhaseDurationSeconds();
            startPhaseTimer();

            // Bot tự động thực hiện hành động sau 1-2 giây
            scheduleBotActions();
        } finally {
            lock.unlock();
        }
    }


    /**
     * Lên lịch bot tự động thực hiện hành động
     */
    private void scheduleBotActions() {
        List<BotPlayer> bots = roomService.getBots(room.getId());
        if (bots.isEmpty()) {
            return;
        }

        // Bot thực hiện hành động sau 1-3 giây ngẫu nhiên
        for (BotPlayer bot : bots) {
            int delay = 1 + (int)(Math.random() * 2); // 1-3 giây
            scheduler.schedule(() -> {
                executeBotAction(bot);
            }, delay, TimeUnit.SECONDS);
        }
    }

    /**
     * Bot thực hiện hành động
     */
    private void executeBotAction(BotPlayer bot) {
        lock.lock();
        try {
            if (!bot.getPlayerState().isAlive()) {
                return;
            }

            List<PlayerState> allPlayers = new ArrayList<>(room.getPlayers().values());
            Integer targetId = bot.chooseAction(this, allPlayers);

            if (targetId == null) {
                return;
            }

            Role role = bot.getPlayerState().getRole();
            switch (currentPhase) {
                case NIGHT_WOLF:
                    if (role == Role.WEREWOLF) {
                        werewolfKill(bot.getBotId(), targetId);
                    }
                    break;
                case NIGHT_SEER:
                    if (role == Role.SEER) {
                        seerCheck(bot.getBotId(), targetId);
                    }
                    break;
                case DAY_VOTE:
                    vote(bot.getBotId(), targetId);
                    break;
                case DAY_HUNTER:
                    // Bot hunter tự chọn người kéo theo ngẫu nhiên
                    if (role == Role.HUNTER && hunterPendingAction && bot.getBotId() == hunterId) {
                        hunterShoot(bot.getBotId(), targetId);
                    }
                    break;
                default:
                    // Bot không có hành động ở các pha khác
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý hành động của sói (giết người)
     */
    public void werewolfKill(int werewolfUserId, int targetUserId) {
        lock.lock();
        try {
            if (currentPhase != GamePhase.NIGHT_WOLF) {
                throw new RuntimeException("Không đúng pha");
            }

            PlayerState werewolf = room.getPlayers().get(werewolfUserId);
            if (werewolf == null || werewolf.getRole() != Role.WEREWOLF || !werewolf.isAlive()) {
                throw new RuntimeException("Bạn không phải sói hoặc đã chết");
            }

            PlayerState target = room.getPlayers().get(targetUserId);
            if (target == null || !target.isAlive()) {
                throw new RuntimeException("Mục tiêu không hợp lệ");
            }

            werewolfKills.put(werewolfUserId, targetUserId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý hành động của tiên tri (soi người)
     */
    public void seerCheck(int seerUserId, int targetUserId) {
        lock.lock();
        try {
            if (currentPhase != GamePhase.NIGHT_SEER) {
                throw new RuntimeException("Không đúng pha");
            }

            PlayerState seer = room.getPlayers().get(seerUserId);
            if (seer == null || seer.getRole() != Role.SEER || !seer.isAlive()) {
                throw new RuntimeException("Bạn không phải tiên tri hoặc đã chết");
            }

            PlayerState target = room.getPlayers().get(targetUserId);
            if (target == null) {
                throw new RuntimeException("Mục tiêu không hợp lệ");
            }

            seerChecks.put(seerUserId, targetUserId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý hành động của bảo vệ
     */
    public void guardProtect(int guardUserId, int targetUserId) {
        lock.lock();
        try {
            if (currentPhase != GamePhase.NIGHT_GUARD) {
                throw new RuntimeException("Không đúng pha");
            }

            PlayerState guard = room.getPlayers().get(guardUserId);
            if (guard == null || guard.getRole() != Role.GUARD || !guard.isAlive()) {
                throw new RuntimeException("Bạn không phải bảo vệ hoặc đã chết");
            }

            guardProtects.put(guardUserId, targetUserId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý hành động của phù thủy
     */
    public void witchAction(int witchUserId, String action, Integer targetUserId) {
        lock.lock();
        try {
            if (currentPhase != GamePhase.NIGHT_WITCH) {
                throw new RuntimeException("Không đúng pha");
            }

            PlayerState witch = room.getPlayers().get(witchUserId);
            if (witch == null || witch.getRole() != Role.WITCH || !witch.isAlive()) {
                throw new RuntimeException("Bạn không phải phù thủy hoặc đã chết");
            }

            if ("SAVE".equals(action)) {
                if (!witch.isHasSavePotion()) throw new RuntimeException("Đã hết thuốc cứu");
                if (witchSaved) throw new RuntimeException("Đã dùng quyền cứu trong lượt này");

                witchSaved = true;
                witch.setHasSavePotion(false);
            } else if ("KILL".equals(action)) {
                if (!witch.isHasKillPotion()) throw new RuntimeException("Đã hết thuốc độc");
                if (targetUserId == null) throw new RuntimeException("Chưa chọn mục tiêu");

                witchKilledId = targetUserId;
                witch.setHasKillPotion(false);
            } else if ("SILENCE".equals(action)) {
                if (targetUserId == null) throw new RuntimeException("Chưa chọn mục tiêu");
                silencedUntilRound.put(targetUserId, roundNumber);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý vote treo cổ
     * Cho phép đổi vote: nếu đã vote rồi thì trừ vote cũ và cộng vote mới
     */
    public void vote(int voterUserId, int targetUserId) {
        lock.lock();
        try {
            if (currentPhase != GamePhase.DAY_VOTE) {
                throw new RuntimeException("Không đúng pha vote");
            }

            PlayerState voter = room.getPlayers().get(voterUserId);
            if (voter == null || !voter.isAlive()) {
                throw new RuntimeException("Bạn không thể vote");
            }

            PlayerState target = room.getPlayers().get(targetUserId);
            if (target == null || !target.isAlive()) {
                throw new RuntimeException("Mục tiêu không hợp lệ");
            }

            // Không cho tự vote bản thân
            if (voterUserId == targetUserId) {
                throw new RuntimeException("Không thể vote cho bản thân");
            }

            // Nếu đã vote cùng người thì bỏ qua
            Integer oldTarget = playerVotes.get(voterUserId);
            if (oldTarget != null && oldTarget == targetUserId) {
                return;
            }

            // Trừ vote cũ (nếu có)
            if (oldTarget != null) {
                votes.put(oldTarget, Math.max(0, votes.getOrDefault(oldTarget, 0) - 1));
            }

            // Cộng vote mới
            playerVotes.put(voterUserId, targetUserId);
            votes.put(targetUserId, votes.getOrDefault(targetUserId, 0) + 1);
            voter.setVoted(true);
            voter.setVoteTarget(targetUserId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Hunter báo thù: kéo một người chết cùng
     */
    public void hunterShoot(int hunterUserId, int targetUserId) {
        lock.lock();
        try {
            if (currentPhase != GamePhase.DAY_HUNTER) {
                throw new RuntimeException("Không đúng pha");
            }
            if (!hunterPendingAction || hunterUserId != hunterId) {
                throw new RuntimeException("Bạn không có quyền hành động này");
            }

            PlayerState target = room.getPlayers().get(targetUserId);
            if (target == null || !target.isAlive()) {
                throw new RuntimeException("Mục tiêu không hợp lệ");
            }
            if (hunterUserId == targetUserId) {
                throw new RuntimeException("Không thể chọn bản thân");
            }

            // Giết mục tiêu
            target.setAlive(false);
            hunterPendingAction = false;

            // Chuyển sang thảo luận ngày
            proceedAfterHunter();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Kết thúc pha hiện tại và chuyển sang pha tiếp theo
     */
    private void endCurrentPhase() {
        lock.lock();
        try {
            switch (currentPhase) {
                case NIGHT_WOLF:
                    // Check for active Guard
                    boolean hasAliveGuard = room.getPlayers().values().stream()
                        .anyMatch(p -> p.getRole() == Role.GUARD && p.isAlive());
                    if (hasAliveGuard) {
                        currentPhase = GamePhase.NIGHT_GUARD;
                        setupNextPhase();
                    } else {
                        // Skip Guard, try Witch
                        transitionToWitchOrSeer();
                    }
                    break;

                case NIGHT_GUARD:
                    // Guard xong, chuyen sang Witch
                    transitionToWitchOrSeer();
                    break;

                case NIGHT_WITCH:
                    // Witch xong, chuyen sang Seer
                    transitionToSeerOrEnd();
                    break;

                case NIGHT_SEER:
                    endNight();
                    break;

                case DAY_CHAT:
                    currentPhase = GamePhase.DAY_VOTE;
                    // Reset trạng thái vote
                    votes.clear();
                    playerVotes.clear();
                    for (PlayerState player : room.getPlayers().values()) {
                        player.setVoted(false);
                        player.setVoteTarget(null);
                    }
                    timeRemaining = room.getPhaseDurationSeconds();
                    startPhaseTimer();
                    scheduleBotActions(); // Bot tự động vote
                    break;

                case DAY_VOTE:
                    executeVote();
                    break;

                case DAY_HUNTER:
                    // Hết giờ mà Hunter chưa hành động -> bỏ qua, tiếp tục game
                    hunterPendingAction = false;
                    proceedAfterHunter();
                    break;

                case WAITING:
                case STARTING:
                case DAY_ANNOUNCE:
                case ENDED:
                    // Không cần xử lý gì ở các pha này
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    private void setupNextPhase() {
        timeRemaining = room.getPhaseDurationSeconds();
        startPhaseTimer();
        scheduleBotActions();
    }

    private void transitionToWitchOrSeer() {
        boolean hasAliveWitch = room.getPlayers().values().stream()
                .anyMatch(p -> p.getRole() == Role.WITCH && p.isAlive());
        if (hasAliveWitch) {
            currentPhase = GamePhase.NIGHT_WITCH;
            setupNextPhase();
        } else {
            transitionToSeerOrEnd();
        }
    }

    private void transitionToSeerOrEnd() {
        boolean hasAliveSeer = room.getPlayers().values().stream()
                .anyMatch(p -> p.getRole() == Role.SEER && p.isAlive());
        if (hasAliveSeer) {
            currentPhase = GamePhase.NIGHT_SEER;
            setupNextPhase();
        } else {
            endNight();
        }
    }

    /**
     * Kết thúc đêm, xử lý kết quả giết người
     */
    private void endNight() {
        // Xử lý vote của sói (majority vote)
        Map<Integer, Integer> killVotes = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : werewolfKills.entrySet()) {
            killVotes.put(entry.getValue(), killVotes.getOrDefault(entry.getValue(), 0) + 1);
        }

        // Tìm người bị giết nhiều nhất
        int maxVotes = 0;
        Integer targetKilled = null;
        for (Map.Entry<Integer, Integer> entry : killVotes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                targetKilled = entry.getKey();
            }
        }

        if (targetKilled != null && maxVotes > 0) {
            // Check Guard protection
            boolean protectedByGuard = guardProtects.containsValue(targetKilled);
            if (protectedByGuard) {
                targetKilled = null; // Saved by guard
            } else if (witchSaved) {
                targetKilled = null; // Saved by witch
            }
        }

        // Apply death
        List<Integer> deadPlayers = new ArrayList<>();
        if (targetKilled != null) {
            deadPlayers.add(targetKilled);
        }
        if (witchKilledId != null) {
            deadPlayers.add(witchKilledId);
        }

        if (!deadPlayers.isEmpty()) {
            killedPlayerId = deadPlayers.get(0);
            for (Integer pid : deadPlayers) {
                PlayerState dead = room.getPlayers().get(pid);
                if (dead != null && dead.isAlive()) {
                    dead.setAlive(false);
                    // Nếu người bị sói giết là Hunter -> kích hoạt quyền báo thù
                    // (chỉ khi bị sói giết trực tiếp, không phải witch killed)
                    if (dead.getRole() == Role.HUNTER && pid.equals(targetKilled)) {
                        hunterPendingAction = true;
                        hunterId = pid;
                    }
                }
            }
        } else {
            killedPlayerId = null;
        }

        // Chuyển sang DAY_ANNOUNCE, rồi DAY_HUNTER (nếu cần), rồi DAY_CHAT
        currentPhase = GamePhase.DAY_ANNOUNCE;
        scheduler.schedule(() -> {
            lock.lock();
            try {
                if (hunterPendingAction) {
                    // Hunter được báo thù trước khi thảo luận
                    currentPhase = GamePhase.DAY_HUNTER;
                    timeRemaining = room.getPhaseDurationSeconds();
                    startPhaseTimer();
                    scheduleBotActions();
                } else {
                    currentPhase = GamePhase.DAY_CHAT;
                    timeRemaining = room.getPhaseDurationSeconds();
                    startPhaseTimer();
                }
            } finally {
                lock.unlock();
            }
        }, 5, TimeUnit.SECONDS); // 5 giây để công bố
    }

    /**
     * Sau khi Hunter hành động (hoặc bỏ qua), kiểm tra win condition rồi sang thảo luận
     */
    private void proceedAfterHunter() {
        // Kiểm tra win condition trước
        int aliveWerewolves = 0;
        int aliveVillagers = 0;
        for (PlayerState player : room.getPlayers().values()) {
            if (player.isAlive()) {
                if (player.getRole() == Role.WEREWOLF) aliveWerewolves++;
                else aliveVillagers++;
            }
        }

        if (aliveWerewolves == 0) {
            winnerTeam = "VILLAGERS";
            endGame();
        } else if (aliveWerewolves >= aliveVillagers) {
            winnerTeam = "WEREWOLVES";
            endGame();
        } else {
            currentPhase = GamePhase.DAY_CHAT;
            timeRemaining = room.getPhaseDurationSeconds();
            startPhaseTimer();
        }
    }

    /**
     * Thực hiện vote và xử lý kết quả
     */
    private void executeVote() {
        // Tìm người bị vote nhiều nhất
        int maxVotes = 0;
        List<Integer> tiedPlayers = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
            int count = entry.getValue();
            if (count > maxVotes) {
                maxVotes = count;
                tiedPlayers.clear();
                tiedPlayers.add(entry.getKey());
            } else if (count == maxVotes && maxVotes > 0) {
                tiedPlayers.add(entry.getKey());
            }
        }

        Integer hangedPlayerId = null;
        if (tiedPlayers.size() == 1 && maxVotes > 0) {
            // Có người bị treo cổ
            hangedPlayerId = tiedPlayers.get(0);
            PlayerState hanged = room.getPlayers().get(hangedPlayerId);
            if (hanged != null) {
                hanged.setAlive(false);
            }
        }
        // Nếu tie thì không ai chết

        // Lưu killedPlayerId để DAY_ANNOUNCE hiện thị
        killedPlayerId = hangedPlayerId;

        // Kiểm tra hunter bị treo cổ: Hunter bị vote KHÔNG được báo thù (chỉ báo thù khi bị sói giết)
        // -> Không kích hoạt hunterPendingAction ở đây

        // Chuyển sang DAY_ANNOUNCE (5 giây), rồi kiểm tra thắng thua
        currentPhase = GamePhase.DAY_ANNOUNCE;
        scheduler.schedule(() -> {
            lock.lock();
            try {
                checkWinCondition();
            } finally {
                lock.unlock();
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * Kiểm tra điều kiện thắng thua
     */
    private void checkWinCondition() {
        int aliveWerewolves = 0;
        int aliveVillagers = 0;

        for (PlayerState player : room.getPlayers().values()) {
            if (player.isAlive()) {
                if (player.getRole() == Role.WEREWOLF) {
                    aliveWerewolves++;
                } else {
                    aliveVillagers++;
                }
            }
        }

        if (aliveWerewolves == 0) {
            // Dân thắng
            winnerTeam = "VILLAGERS";
            endGame();
        } else if (aliveWerewolves >= aliveVillagers) {
            // Sói thắng
            winnerTeam = "WEREWOLVES";
            endGame();
        } else {
            // Tiếp tục game, chuyển sang đêm tiếp theo
            scheduler.schedule(() -> {
                startNight();
            }, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * Kết thúc game
     */
    private void endGame() {
        if (matchRewardsApplied) {
            return;
        }
        matchRewardsApplied = true;

        currentPhase = GamePhase.ENDED;
        room.setStatus("ENDED");
        roomService.updateRoomStatus(room.getId(), "ENDED");
        applyMatchRewards();
    }

    private void applyMatchRewards() {
        Set<Integer> participants = new HashSet<>();
        Set<Integer> winners = new HashSet<>();
        boolean villagersWin = "VILLAGERS".equals(winnerTeam);
        boolean werewolvesWin = "WEREWOLVES".equals(winnerTeam);

        for (PlayerState player : room.getPlayers().values()) {
            int userId = player.getUserId();
            if (roomService.isBot(userId)) {
                continue;
            }
            participants.add(userId);
            if (villagersWin && player.getRole() != Role.WEREWOLF) {
                winners.add(userId);
            } else if (werewolvesWin && player.getRole() == Role.WEREWOLF) {
                winners.add(userId);
            }
        }

        if (!participants.isEmpty()) {
            userRepository.applyMatchRewards(winners, participants);
        }
    }

    /**
     * Bắt đầu timer cho pha hiện tại
     */
    private void startPhaseTimer() {
        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                if (timeRemaining > 0) {
                    timeRemaining--;
                } else {
                    endCurrentPhase();
                }
            } finally {
                lock.unlock();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Lấy game state hiện tại
     */
    public GameStateUpdate getGameState(int userId) {
        lock.lock();
        try {
            GameStateUpdate update = new GameStateUpdate();
            update.setPhase(currentPhase);
            update.setRoundNumber(roundNumber);
            update.setTimeRemaining(timeRemaining);
            update.setKilledPlayerId(killedPlayerId);
            update.setWinnerTeam(winnerTeam);

            // Convert players
            List<PlayerDTO> playerDTOs = new ArrayList<>();
            for (PlayerState player : room.getPlayers().values()) {
                PlayerDTO dto = new PlayerDTO(
                    player.getUserId(),
                    player.getDisplayName(),
                    player.isReady(),
                    player.getRole(),
                    player.isAlive(),
                    player.getUserId() == room.getHostUserId()
                );
                playerDTOs.add(dto);
            }
            update.setPlayers(playerDTOs);

            // Set votes (luôn gửi để client hiện tally realtime)
            update.setVotes(new HashMap<>(votes));

            // Kiểm tra user có thể hành động không
            PlayerState player = room.getPlayers().get(userId);
            boolean canAct = false;
            if (player != null && player.isAlive()) {
                switch (currentPhase) {
                    case NIGHT_WOLF:
                        canAct = player.getRole() == Role.WEREWOLF && !werewolfKills.containsKey(userId);
                        break;
                    case NIGHT_GUARD:
                        canAct = player.getRole() == Role.GUARD && !guardProtects.containsKey(userId);
                        break;
                    case NIGHT_WITCH:
                        canAct = player.getRole() == Role.WITCH;
                        if (canAct) {
                            // Tính người bị sói giết để hiện cho Witch
                            Map<Integer, Integer> killVotes = new HashMap<>();
                            for (Map.Entry<Integer, Integer> entry : werewolfKills.entrySet()) {
                                killVotes.put(entry.getValue(), killVotes.getOrDefault(entry.getValue(), 0) + 1);
                            }
                            int maxKillVotes = 0;
                            Integer targetKilled = null;
                            for (Map.Entry<Integer, Integer> entry : killVotes.entrySet()) {
                                if (entry.getValue() > maxKillVotes) {
                                    maxKillVotes = entry.getValue();
                                    targetKilled = entry.getKey();
                                }
                            }
                            if (targetKilled != null) {
                                update.setKilledPlayerId(targetKilled); // Reveal to Witch
                            }
                        }
                        break;
                    case NIGHT_SEER:
                        canAct = player.getRole() == Role.SEER && !seerChecks.containsKey(userId);
                        break;
                    case DAY_VOTE:
                        canAct = true; // Luôn cho phép vote / đổi vote nếu còn sống
                        break;
                    case DAY_HUNTER:
                        canAct = player.getRole() == Role.HUNTER && hunterPendingAction && userId == hunterId;
                        break;
                    case WAITING:
                    case STARTING:
                    case DAY_ANNOUNCE:
                    case DAY_CHAT:
                    case ENDED:
                        canAct = false;
                        break;
                }
            }
            update.setCanAct(canAct);
            update.setHunterMustAct(hunterPendingAction && player != null && player.getRole() == Role.HUNTER && userId == hunterId);

            return update;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lấy kết quả soi của tiên tri
     */
    public boolean getSeerResult(int seerUserId, int targetUserId) {
        PlayerState target = room.getPlayers().get(targetUserId);
        return target != null && target.getRole() == Role.WEREWOLF;
    }

    public boolean canChat(int userId) {
        lock.lock();
        try {
            if (currentPhase != GamePhase.DAY_CHAT && currentPhase != GamePhase.DAY_VOTE) {
                return true;
            }
            int mutedRound = silencedUntilRound.getOrDefault(userId, -1);
            return mutedRound < roundNumber;
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
