package com.werewolf.server.service;

import com.werewolf.server.entity.PlayerState;
import com.werewolf.server.entity.Room;
import com.werewolf.server.entity.User;
import com.werewolf.server.repository.RoomMemberRepository;
import com.werewolf.server.repository.RoomRepository;
import com.werewolf.server.repository.UserRepository;
import com.werewolf.shared.dto.PlayerDTO;
import com.werewolf.shared.dto.RoomDTO;
import com.werewolf.shared.enums.Role;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý rooms
 */
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    
    // In-memory cache các room đang active
    private final Map<Integer, Room> activeRooms = new ConcurrentHashMap<>();
    // Map roomId -> List of bots
    private final Map<Integer, List<BotPlayer>> roomBots = new ConcurrentHashMap<>();

    public RoomService() {
        this.roomRepository = new RoomRepository();
        this.roomMemberRepository = new RoomMemberRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * Tạo room mới
     */
    public Room createRoom(String roomName, String password, int maxPlayers, int phaseDurationSeconds, int hostUserId, com.werewolf.shared.dto.RoleConfig roleConfig) {
        // Validate maxPlayers
        if (maxPlayers < 4 || maxPlayers > 16) {
            throw new RuntimeException("Số người chơi tối đa phải từ 4 đến 16");
        }
        
        String passwordHash = null;
        if (password != null && !password.isEmpty()) {
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        }

        Room room = new Room();
        room.setName(roomName);
        room.setPasswordHash(passwordHash);
        room.setHostUserId(hostUserId);
        room.setMaxPlayers(maxPlayers);
        room.setPhaseDurationSeconds(phaseDurationSeconds);
        room.setRoleConfig(roleConfig);
        room.setStatus("WAITING");

        room = roomRepository.create(room);
        
        // Đưa room vào activeRooms TRƯỚC khi thêm người chơi
        activeRooms.put(room.getId(), room);
        
        // Thêm host vào room
        addPlayer(room.getId(), hostUserId);
        
        return room;
    }

    /**
     * Thêm player vào room
     */
    public void addPlayer(int roomId, int userId) {
        Room room = getRoom(roomId);
        if (room == null) {
            throw new RuntimeException("Không tìm thấy phòng");
        }
        if (room.isFull()) {
            throw new RuntimeException("Phòng đã đầy");
        }
        if (room.getStatus().equals("PLAYING")) {
            throw new RuntimeException("Phòng đã bắt đầu game");
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy user");
        }

        PlayerState playerState = new PlayerState(userId, user.getDisplayName());
        room.getPlayers().put(userId, playerState);
        roomMemberRepository.addMember(roomId, userId);
    }

    /**
     * Xóa player khỏi room.
     * Nếu không còn người chơi thật nào, phòng sẽ tự động bị xóa.
     */
    public void removePlayer(int roomId, int userId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.getPlayers().remove(userId);
            
            // Nếu là bot, xóa khỏi danh sách bot
            if (isBot(userId)) {
                List<BotPlayer> bots = roomBots.get(roomId);
                if (bots != null) {
                    bots.removeIf(bot -> bot.getBotId() == userId);
                }
            }
        }
        
        // Chỉ xóa khỏi database cho người chơi thật, không xóa bot
        if (!isBot(userId)) {
            roomMemberRepository.removeMember(roomId, userId);
        }
        
        // Kiểm tra nếu phòng không còn người chơi thật -> tự động xóa phòng
        if (room != null) {
            long realPlayerCount = room.getPlayers().keySet().stream()
                    .filter(id -> !isBot(id))
                    .count();
            if (realPlayerCount == 0) {
                System.out.println("[RoomService] Phòng " + roomId + " không còn người chơi, tự động xóa.");
                deleteRoom(roomId);
            }
        }
    }

    /**
     * Xóa phòng hoàn toàn khỏi cache và database
     */
    public void deleteRoom(int roomId) {
        activeRooms.remove(roomId);
        roomBots.remove(roomId);
        try {
            roomRepository.deleteById(roomId);
            System.out.println("[RoomService] Đã xóa phòng " + roomId + " khỏi database.");
        } catch (Exception e) {
            System.err.println("[RoomService] Lỗi khi xóa phòng " + roomId + ": " + e.getMessage());
        }
    }

    /**
     * Set ready status
     */
    public void setReady(int roomId, int userId, boolean ready) {
        Room room = getRoom(roomId);
        if (room == null) {
            throw new RuntimeException("Không tìm thấy phòng");
        }
        PlayerState player = room.getPlayers().get(userId);
        if (player == null) {
            throw new RuntimeException("Bạn không ở trong phòng");
        }
        player.setReady(ready);
        
        // Chỉ update database cho người chơi thật, không update cho bot
        if (!isBot(userId)) {
            roomMemberRepository.updateReady(roomId, userId, ready);
        }
    }

    /**
     * Kiểm tra password room
     */
    public boolean checkPassword(int roomId, String password) {
        Room room = getRoom(roomId);
        if (room == null) {
            return false;
        }
        if (!room.hasPassword()) {
            return true;
        }
        if (password == null || password.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(password, room.getPasswordHash());
    }

    /**
     * Lấy room từ cache hoặc DB
     */
    public Room getRoom(int roomId) {
        Room room = activeRooms.get(roomId);
        if (room == null) {
            room = roomRepository.findById(roomId);
            if (room != null && !room.getStatus().equals("ENDED")) {
                activeRooms.put(roomId, room);
            }
        }
        return room;
    }

    /**
     * Lấy danh sách room
     */
    public List<RoomDTO> getRoomList(String filter) {
        // TODO: Query từ DB thay vì chỉ lấy từ cache
        List<RoomDTO> result = new ArrayList<>();
        for (Room room : activeRooms.values()) {
            if (filter == null || filter.isEmpty() || room.getStatus().equals(filter)) {
                result.add(toDTO(room));
            }
        }
        return result;
    }

    /**
     * Convert Room entity sang RoomDTO
     */
    public RoomDTO toDTO(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setHostUserId(room.getHostUserId());
        User host = userRepository.findById(room.getHostUserId());
        if (host != null) {
            dto.setHostDisplayName(host.getDisplayName());
        }
        dto.setMaxPlayers(room.getMaxPlayers());
        dto.setCurrentPlayers(room.getCurrentPlayers());
        dto.setPhaseDurationSeconds(room.getPhaseDurationSeconds());
        dto.setStatus(room.getStatus());
        dto.setStatus(room.getStatus());
        dto.setHasPassword(room.hasPassword());
        dto.setRoleConfig(room.getRoleConfig());

        // Convert players
        List<PlayerDTO> playerDTOs = new ArrayList<>();
        for (PlayerState player : room.getPlayers().values()) {
            PlayerDTO playerDTO = new PlayerDTO(
                player.getUserId(),
                player.getDisplayName(),
                player.isReady(),
                player.getRole(),
                player.isAlive(),
                player.getUserId() == room.getHostUserId()
            );
            playerDTOs.add(playerDTO);
        }
        dto.setPlayers(playerDTOs);

        return dto;
    }

    /**
     * Chia vai trò ngẫu nhiên cho tất cả players trong room
     */
    public void assignRoles(Room room) {
        List<PlayerState> players = new ArrayList<>(room.getPlayers().values());
        int playerCount = players.size();
        com.werewolf.shared.dto.RoleConfig config = room.getRoleConfig();
        
        // Shuffle players
        java.util.Collections.shuffle(players);
        
        int index = 0;
        
        // 1. Assign Werewolves
        int wolfCount = config != null ? config.getWerewolfCount() : Math.max(1, playerCount / 3);
        for (int i = 0; i < wolfCount && index < playerCount; i++) {
            players.get(index++).setRole(Role.WEREWOLF);
        }
        
        // 2. Assign Seer
        if (config == null || config.isHasSeer()) {
            if (index < playerCount) players.get(index++).setRole(Role.SEER);
        }
        
        // 3. Assign Guard
        if (config != null && config.isHasGuard()) {
            if (index < playerCount) players.get(index++).setRole(Role.GUARD);
        }
        
        // 4. Assign Witch
        if (config != null && config.isHasWitch()) {
            if (index < playerCount) {
                PlayerState witch = players.get(index++);
                witch.setRole(Role.WITCH);
                witch.setHasSavePotion(true);
                witch.setHasKillPotion(true);
            }
        }
        
        // 5. Assign Hunter
        if (config != null && config.isHasHunter()) {
            if (index < playerCount) players.get(index++).setRole(Role.HUNTER);
        }
        
        // 6. Assign Villagers
        while (index < playerCount) {
            players.get(index++).setRole(Role.VILLAGER);
        }
    }

    /**
     * Cập nhật status của room
     */
    public void updateRoomStatus(int roomId, String status) {
        roomRepository.updateStatus(roomId, status);
        Room room = activeRooms.get(roomId);
        if (room != null) {
            room.setStatus(status);
        }
    }
    
    /**
     * Thêm bot vào room
     */
    public void addBotsToRoom(Room room, int count) {
        if (count <= 0) return;
        if (room == null) return;
        
        try {
            List<BotPlayer> bots = roomBots.computeIfAbsent(room.getId(), k -> new ArrayList<>());
            
            for (int i = 0; i < count; i++) {
                // Kiểm tra phòng còn chỗ không
                if (room.isFull()) {
                    System.out.println("[RoomService] Phòng đã đầy, dừng thêm bot");
                    break;
                }
                
                BotPlayer bot = BotPlayer.createBot();
                bots.add(bot);
                
                // Thêm bot vào room
                room.getPlayers().put(bot.getBotId(), bot.getPlayerState());
                
                // Bot tự động ready
                bot.autoReady();
                
                System.out.println("[RoomService] Đã thêm bot " + bot.getDisplayName() + " (ID: " + bot.getBotId() + ") vào phòng " + room.getId());
            }
        } catch (Exception e) {
            System.err.println("[RoomService] Lỗi khi thêm bot vào phòng: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi thêm bot: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tự động thêm bot nếu chưa đủ người chơi trong phòng
     * Thêm bot để đủ ít nhất 4 người chơi (số tối thiểu để chơi được)
     */
    public void autoAddBotsIfNeeded(int roomId) {
        try {
            Room room = getRoom(roomId);
            if (room == null) {
                return;
            }
            
            // Chỉ thêm bot khi phòng đang chờ (WAITING) và chưa bắt đầu game
            if (!"WAITING".equals(room.getStatus())) {
                return;
            }
            
            int currentTotal = room.getCurrentPlayers();
            int minPlayers = Math.min(4, room.getMaxPlayers()); // Đảm bảo không vượt quá maxPlayers
            
            // Thêm bot nếu chưa đủ 4 người chơi
            if (currentTotal < minPlayers) {
                int botsToAdd = minPlayers - currentTotal;
                
                // Không thêm quá số chỗ còn lại
                int availableSlots = room.getMaxPlayers() - currentTotal;
                botsToAdd = Math.min(botsToAdd, availableSlots);
                
                if (botsToAdd > 0) {
                    System.out.println("[RoomService] Tự động thêm " + botsToAdd + " bot vào phòng " + roomId + 
                                     " (hiện tại: " + currentTotal + "/" + room.getMaxPlayers() + ")");
                    addBotsToRoom(room, botsToAdd);
                } else {
                    System.out.println("[RoomService] Không thể thêm bot: phòng đã đầy");
                }
            }
        } catch (Exception e) {
            System.err.println("[RoomService] Lỗi khi tự động thêm bot: " + e.getMessage());
            e.printStackTrace();
            // Không throw exception để không làm crash việc tạo phòng
        }
    }
    
    /**
     * Lấy danh sách bot trong room
     */
    public List<BotPlayer> getBots(int roomId) {
        return roomBots.getOrDefault(roomId, new ArrayList<>());
    }
    
    /**
     * Kiểm tra xem userId có phải bot không
     */
    public boolean isBot(int userId) {
        return userId <= -1000; // Bot IDs bắt đầu từ -1000
    }
}
