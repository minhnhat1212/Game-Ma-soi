package com.werewolf.server.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolf.server.entity.Room;
import com.werewolf.server.entity.User;
import com.werewolf.server.service.AuthService;
import com.werewolf.server.service.GameEngine;
import com.werewolf.server.service.RoomService;
import com.werewolf.shared.dto.*;
import com.werewolf.shared.enums.ErrorCode;
import com.werewolf.shared.enums.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Xử lý kết nối từ một client
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final RoomService roomService;
    private final Map<Integer, GameEngine> gameEngines;
    
    private User currentUser;
    private Room currentRoom;
    private GameEngine currentGameEngine;
    private PrintWriter out;
    private BufferedReader in;
    private final ScheduledExecutorService gameUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean gameUpdateTaskStarted = false;
    private volatile boolean userProgressSent = false;

    public ClientHandler(Socket socket, AuthService authService, RoomService roomService, 
                        Map<Integer, GameEngine> gameEngines) {
        this.socket = socket;
        this.objectMapper = new ObjectMapper();
        this.authService = authService;
        this.roomService = roomService;
        this.gameEngines = gameEngines;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("[Server] Client handler đã sẵn sàng: " + clientAddress);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    Message message = objectMapper.readValue(inputLine, Message.class);
                    System.out.println("[Server] Nhận message từ " + clientAddress + ": " + message.getType());
                    handleMessage(message);
                } catch (Throwable e) {
                    System.err.println("[Server] Lỗi parse message từ " + clientAddress + ": " + e.getMessage());
                    e.printStackTrace();
                    sendError(ErrorCode.INVALID_REQUEST);
                }
            }
            System.out.println("[Server] Client đóng kết nối chủ động: " + clientAddress);
        } catch (IOException e) {
            System.out.println("[Server] Client ngắt kết nối: " + clientAddress + " | reason: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Message message) {
        try {
            switch (message.getType()) {
                case "LOGIN_REQUEST":
                    handleLogin((LoginRequest) message);
                    break;
                case "REGISTER_REQUEST":
                    handleRegister((RegisterRequest) message);
                    break;
                case "CREATE_ROOM_REQUEST":
                    handleCreateRoom((CreateRoomRequest) message);
                    break;
                case "JOIN_ROOM_REQUEST":
                    handleJoinRoom((JoinRoomRequest) message);
                    break;
                case "LEAVE_ROOM_REQUEST":
                    handleLeaveRoom((LeaveRoomRequest) message);
                    break;
                case "READY_REQUEST":
                    handleReady((ReadyRequest) message);
                    break;
                case "START_GAME_REQUEST":
                    handleStartGame((StartGameRequest) message);
                    break;
                case "ADD_BOT_REQUEST":
                    handleAddBot((AddBotRequest) message);
                    break;
                case "GAME_ACTION_REQUEST":
                    handleGameAction((GameActionRequest) message);
                    break;
                case "CHAT_REQUEST":
                    handleChat((ChatRequest) message);
                    break;
                case "GET_ROOM_LIST_REQUEST":
                    handleGetRoomList((GetRoomListRequest) message);
                    break;
                default:
                    sendError(ErrorCode.INVALID_REQUEST);
            }
        } catch (Throwable e) {
            System.err.println("[Server] Lỗi xử lý message type=" + (message != null ? message.getType() : "null") + ": " + e.getMessage());
            e.printStackTrace();
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void handleLogin(LoginRequest request) {
        try {
            User user = authService.login(request.getUsername(), request.getPassword());
            currentUser = user;
            LoginResponse response = new LoginResponse(true, authService.toDTO(user), null);
            sendMessage(response);
        } catch (Exception e) {
            LoginResponse response = new LoginResponse(false, null, e.getMessage());
            sendMessage(response);
        }
    }

    private void handleRegister(RegisterRequest request) {
        try {
            User user = authService.register(request.getUsername(), request.getPassword(), request.getDisplayName());
            currentUser = user;
            LoginResponse response = new LoginResponse(true, authService.toDTO(user), null);
            sendMessage(response);
        } catch (Exception e) {
            LoginResponse response = new LoginResponse(false, null, e.getMessage());
            sendMessage(response);
        }
    }

    private void handleCreateRoom(CreateRoomRequest request) {
        if (currentUser == null) {
            sendError(ErrorCode.UNAUTHORIZED);
            return;
        }

        try {
            Room room = roomService.createRoom(
                request.getRoomName(),
                request.getPassword(),
                request.getMaxPlayers(),
                request.getPhaseDurationSeconds(),
                currentUser.getId(),
                request.getRoleConfig()
            );
            currentRoom = room;
            
            // Không tự động thêm bot - host sẽ thêm bot thủ công bằng nút
            
            RoomUpdate update = new RoomUpdate(roomService.toDTO(currentRoom));
            sendMessage(update);
        } catch (Exception e) {
            System.err.println("[ClientHandler] Lỗi khi tạo phòng: " + e.getMessage());
            e.printStackTrace();
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void handleJoinRoom(JoinRoomRequest request) {
        if (currentUser == null) {
            sendError(ErrorCode.UNAUTHORIZED);
            return;
        }

        try {
            if (!roomService.checkPassword(request.getRoomId(), request.getPassword())) {
                sendError(ErrorCode.ROOM_PASSWORD_INCORRECT);
                return;
            }

            roomService.addPlayer(request.getRoomId(), currentUser.getId());
            currentRoom = roomService.getRoom(request.getRoomId());
            
            // Không tự động thêm bot - host sẽ thêm bot thủ công bằng nút
            
            // GameEngine chỉ tạo khi host START_GAME — tránh engine "rác" bị ghi đè khiến client join
            // vẫn giữ reference engine cũ (WAITING) trong khi map đã có engine mới.
            currentGameEngine = gameEngines.get(request.getRoomId());
            ensureGameUpdateTask();

            RoomUpdate update = new RoomUpdate(roomService.toDTO(currentRoom));
            sendMessage(update);
        } catch (Exception e) {
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void handleLeaveRoom(LeaveRoomRequest request) {
        if (currentUser == null || currentRoom == null) {
            sendError(ErrorCode.NOT_IN_ROOM);
            return;
        }

        try {
            int roomId = currentRoom.getId();
            roomService.removePlayer(roomId, currentUser.getId());
            currentRoom = null;
            currentGameEngine = null;
            
            // Nếu phòng đã bị xóa (không còn ai), dọn gameEngine
            if (roomService.getRoom(roomId) == null) {
                gameEngines.remove(roomId);
                System.out.println("[ClientHandler] Đã xóa GameEngine của phòng " + roomId);
            } else {
                // Phòng vẫn còn: tự động thêm bot nếu cần
                roomService.autoAddBotsIfNeeded(roomId);
            }
            
            userProgressSent = false;
            sendMessage(new SystemMessage("Đã rời phòng"));
        } catch (Exception e) {
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void handleReady(ReadyRequest request) {
        if (currentUser == null || currentRoom == null) {
            sendError(ErrorCode.NOT_IN_ROOM);
            return;
        }

        try {
            roomService.setReady(currentRoom.getId(), currentUser.getId(), request.isReady());
            RoomUpdate update = new RoomUpdate(roomService.toDTO(currentRoom));
            sendMessage(update);
        } catch (Exception e) {
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void handleAddBot(AddBotRequest request) {
        if (currentUser == null || currentRoom == null) {
            sendError(ErrorCode.NOT_IN_ROOM);
            return;
        }

        if (currentRoom.getHostUserId() != currentUser.getId()) {
            sendError(ErrorCode.NOT_HOST);
            return;
        }

        try {
            int count = request.getCount();
            if (count <= 0) {
                count = 1; // Default to 1 bot
            }
            
            // Kiểm tra phòng còn chỗ không
            int availableSlots = currentRoom.getMaxPlayers() - currentRoom.getCurrentPlayers();
            if (availableSlots <= 0) {
                sendError(ErrorCode.SERVER_ERROR);
                return;
            }
            
            // Thêm bot (không vượt quá số chỗ còn lại)
            int botsToAdd = Math.min(count, availableSlots);
            roomService.addBotsToRoom(currentRoom, botsToAdd);
            
            // Cập nhật lại room sau khi thêm bot
            currentRoom = roomService.getRoom(currentRoom.getId());
            
            // Gửi room update
            RoomUpdate update = new RoomUpdate(roomService.toDTO(currentRoom));
            sendMessage(update);
            sendMessage(new SystemMessage("Đã thêm " + botsToAdd + " bot vào phòng"));
        } catch (Exception e) {
            System.err.println("[ClientHandler] Lỗi khi thêm bot: " + e.getMessage());
            e.printStackTrace();
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void handleStartGame(StartGameRequest request) {
        if (currentUser == null || currentRoom == null) {
            sendError(ErrorCode.NOT_IN_ROOM);
            return;
        }

        if (currentRoom.getHostUserId() != currentUser.getId()) {
            sendError(ErrorCode.NOT_HOST);
            return;
        }

        try {
            System.out.println("[ClientHandler] START_GAME begin roomId=" + currentRoom.getId() + ", host=" + currentUser.getUsername());
            // Refresh trạng thái phòng mới nhất trước khi start (ready/bot/status)
            currentRoom = roomService.getRoom(currentRoom.getId());
            if (currentRoom == null) {
                sendError(ErrorCode.NOT_IN_ROOM);
                return;
            }
            System.out.println("[ClientHandler] START_GAME room status=" + currentRoom.getStatus()
                + ", players=" + currentRoom.getCurrentPlayers());

            syncGameEngineFromMap();
            if (currentGameEngine == null) {
                currentGameEngine = new GameEngine(currentRoom, roomService);
                gameEngines.put(currentRoom.getId(), currentGameEngine);
            }
            currentGameEngine.startGame();
            System.out.println("[ClientHandler] START_GAME engine started successfully for roomId=" + currentRoom.getId());
            currentRoom = roomService.getRoom(currentRoom.getId());
            sendMessage(new SystemMessage("Game bắt đầu!"));
            
            userProgressSent = false; // reset cho ván mới
            
            // Gửi game state update
            GameStateUpdate update = currentGameEngine.getGameState(currentUser.getId());
            sendMessage(update);
            ensureGameUpdateTask();
        } catch (Exception e) {
            System.err.println("[ClientHandler] Không thể bắt đầu game: " + e.getMessage());
            sendMessage(new SystemMessage("Không thể bắt đầu game: " + e.getMessage()));
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void handleGameAction(GameActionRequest request) {
        if (currentUser == null || currentRoom == null) {
            sendError(ErrorCode.GAME_NOT_STARTED);
            return;
        }
        syncGameEngineFromMap();
        if (currentGameEngine == null) {
            sendError(ErrorCode.GAME_NOT_STARTED);
            return;
        }

        try {
            switch (request.getAction()) {
                case "KILL":
                    currentGameEngine.werewolfKill(currentUser.getId(), request.getTargetUserId());
                    break;
                case "VOTE":
                    currentGameEngine.vote(currentUser.getId(), request.getTargetUserId());
                    break;
                case "SEER_CHECK":
                    currentGameEngine.seerCheck(currentUser.getId(), request.getTargetUserId());
                    boolean isWerewolf = currentGameEngine.getSeerResult(currentUser.getId(), request.getTargetUserId());
                    sendMessage(new SystemMessage("Kết quả soi: " + (isWerewolf ? "⚠️ Là Sói!" : "✅ Không phải Sói")));
                    break;
                case "SEER_INTUITION":
                    // Trực giác: soi 2 người cùng lúc (targetUserId = id1, target2UserId = id2 trong JSON extra)
                    int target1 = request.getTargetUserId();
                    int target2 = request.getTarget2UserId();
                    boolean hasSroi = currentGameEngine.seerIntuitionCheck(currentUser.getId(), target1, target2);
                    sendMessage(new SystemMessage("🔮 Trực giác: Trong 2 người được chọn — "
                        + (hasSroi ? "⚠️ CÓ Sói!" : "✅ KHÔNG có Sói")));
                    break;
                case "GUARD_PROTECT":
                    currentGameEngine.guardProtect(currentUser.getId(), request.getTargetUserId());
                    sendMessage(new SystemMessage("🛡️ Đã bảo vệ thành công!"));
                    break;
                case "GUARD_SELF":
                    currentGameEngine.guardSelfProtect(currentUser.getId());
                    sendMessage(new SystemMessage("🛡️ Đã tự bảo vệ bản thân đêm nay!"));
                    break;
                case "WITCH_KILL":
                    currentGameEngine.witchAction(currentUser.getId(), "KILL", request.getTargetUserId());
                    sendMessage(new SystemMessage("🧪 Đã dùng thuốc độc!"));
                    break;
                case "SAVE":
                case "WITCH_SAVE":
                    currentGameEngine.witchAction(currentUser.getId(), "SAVE", request.getTargetUserId());
                    sendMessage(new SystemMessage("💊 Đã dùng thuốc cứu!"));
                    break;
                case "WITCH_SILENCE":
                    currentGameEngine.witchAction(currentUser.getId(), "SILENCE", request.getTargetUserId());
                    sendMessage(new SystemMessage("🤫 Đã câm chat người này trong lượt ngày hôm nay!"));
                    break;
                case "WITCH_REVIVE":
                    currentGameEngine.witchAction(currentUser.getId(), "REVIVE", request.getTargetUserId());
                    sendMessage(new SystemMessage("✨ Đã dùng Hồi sinh tạm — người đó có thể nói 1 lượt!"));
                    break;
                case "WOLF_FRAME":
                    currentGameEngine.werewolfFrame(currentUser.getId(), request.getTargetUserId());
                    sendMessage(new SystemMessage("🎭 Đã frame người này — Tiên Tri sẽ thấy họ là Sói!"));
                    break;
                case "HUNTER_SHOOT":
                    currentGameEngine.hunterShoot(currentUser.getId(), request.getTargetUserId());
                    break;
            }
            
            // Gửi game state update
            GameStateUpdate update = currentGameEngine.getGameState(currentUser.getId());
            sendMessage(update);
        } catch (Exception e) {
            sendError(ErrorCode.INVALID_ACTION);
        }
    }

    private void handleChat(ChatRequest request) {
        if (currentUser == null) {
            sendError(ErrorCode.UNAUTHORIZED);
            return;
        }

        syncGameEngineFromMap();
        if (currentGameEngine != null && !currentGameEngine.canChat(currentUser.getId())) {
            sendMessage(new SystemMessage("Bạn đang bị câm chat trong lượt này."));
            return;
        }

        // TODO: Broadcast chat message đến tất cả clients trong room
        ChatMessage chatMsg = new ChatMessage(
            currentUser.getId(),
            currentUser.getDisplayName(),
            request.getContent(),
            MessageType.PUBLIC
        );
        sendMessage(chatMsg);
    }

    private void handleGetRoomList(GetRoomListRequest request) {
        try {
            java.util.List<RoomDTO> rooms = roomService.getRoomList(request.getFilter());
            RoomListResponse response = new RoomListResponse(rooms);
            sendMessage(response);
        } catch (Exception e) {
            sendError(ErrorCode.SERVER_ERROR);
        }
    }

    private void sendMessage(Message message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            out.println(json);
        } catch (Exception e) {
            System.err.println("Lỗi gửi message: " + e.getMessage());
        }
    }

    private void sendError(ErrorCode errorCode) {
        sendMessage(new ErrorResponse(errorCode));
    }

    private void ensureGameUpdateTask() {
        if (gameUpdateTaskStarted) {
            return;
        }

        gameUpdateTaskStarted = true;
        gameUpdateScheduler.scheduleAtFixedRate(() -> {
            try {
                if (currentUser == null || currentRoom == null) {
                    return;
                }

                // Luôn lấy engine từ map — tránh giữ reference engine cũ sau khi host tạo engine mới.
                syncGameEngineFromMap();
                if (currentGameEngine == null) {
                    return;
                }

                // Refresh trạng thái object nếu endGame() đã cập nhật
                currentRoom = roomService.getRoom(currentRoom.getId());

                if (!"PLAYING".equals(currentRoom.getStatus()) && !"ENDED".equals(currentRoom.getStatus())) {
                    return;
                }

                GameStateUpdate update = currentGameEngine.getGameState(currentUser.getId());
                sendMessage(update);

                // Push progression một lần khi game kết thúc
                if ("ENDED".equals(currentRoom.getStatus()) && !userProgressSent) {
                    UserDTO updated = authService.getUserDTOById(currentUser.getId());
                    if (updated != null) {
                        sendMessage(new UserProgressUpdate(updated));
                        sendMessage(new SystemMessage("Hoàn thành ván đấu!"));
                    }
                    userProgressSent = true;
                }
            } catch (Exception ignored) {
                // Keep scheduler alive even if one tick fails.
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /** Đồng bộ currentGameEngine với map (một phòng = một engine). */
    private void syncGameEngineFromMap() {
        if (currentRoom == null) {
            currentGameEngine = null;
            return;
        }
        currentGameEngine = gameEngines.get(currentRoom.getId());
    }

    private void cleanup() {
        try {
            String clientAddress = socket.getRemoteSocketAddress().toString();
            if (currentUser != null) {
                System.out.println("[Server] User " + currentUser.getUsername() + " đăng xuất");
                authService.logout(currentUser.getId());
            }
            if (currentRoom != null && currentUser != null) {
                int roomId = currentRoom.getId();
                roomService.removePlayer(roomId, currentUser.getId());
                // Nếu phòng đã bị xóa (không còn ai), dọn gameEngine
                if (roomService.getRoom(roomId) == null) {
                    gameEngines.remove(roomId);
                    System.out.println("[ClientHandler] Phòng " + roomId + " đã được xóa sau khi ngắt kết nối.");
                }
            }
            gameUpdateScheduler.shutdownNow();
            socket.close();
            System.out.println("[Server] Đã đóng kết nối với: " + clientAddress);
        } catch (IOException e) {
            System.err.println("[Server] Lỗi cleanup: " + e.getMessage());
        }
    }
}
