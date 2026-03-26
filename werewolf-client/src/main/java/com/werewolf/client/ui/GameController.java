package com.werewolf.client.ui;

import com.werewolf.client.network.NetworkClient;
import com.werewolf.shared.dto.*;
import com.werewolf.shared.enums.GamePhase;
import com.werewolf.shared.enums.Role;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Controller cho màn hình chơi game
 */
public class GameController {
    @FXML
    private Label phaseLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private Label roundLabel;
    @FXML
    private Label phaseDescriptionLabel;
    @FXML
    private FlowPane playerPane;
    @FXML
    private VBox actionPanel;
    @FXML
    private Label actionDescriptionLabel;
    @FXML
    private ListView<PlayerDTO> targetList;
    @FXML
    private Button submitActionButton;
    @FXML
    private VBox voteResultPanel;
    @FXML
    private Label voteResultLabel;
    @FXML
    private VBox gameEndPanel;
    @FXML
    private Label winnerLabel;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField chatInput;
    @FXML
    private Label myRoleLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label turnInfoLabel;

    private NetworkClient networkClient;
    private Stage stage;
    private UserDTO currentUser;
    private RoomDTO currentRoom;
    private GameStateUpdate currentGameState;
    private Role myRole;
    private int timeRemaining = 0;
    /** Ghi nhận pha đã log vào chat (tránh spam mỗi giây). */
    private GamePhase lastChatPhase = null;

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
    }

    public void initialize() {
        // Hidden overlays should not keep layout footprint.
        actionPanel.managedProperty().bind(actionPanel.visibleProperty());
        voteResultPanel.managedProperty().bind(voteResultPanel.visibleProperty());
        gameEndPanel.managedProperty().bind(gameEndPanel.visibleProperty());

        // Setup target list cell factory
        targetList.setCellFactory(listView -> new ListCell<PlayerDTO>() {
            @Override
            protected void updateItem(PlayerDTO player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) {
                    setText(null);
                } else {
                    setText(player.getDisplayName() + (player.isAlive() ? " (Sống)" : " (Chết)"));
                }
            }
        });
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
        
        // Set message listener
        networkClient.setMessageListener(message -> {
            Platform.runLater(() -> {
                try {
                    handleServerMessage(message);
                } catch (Throwable uiError) {
                    System.err.println("[GameController] Lỗi xử lý message: " + uiError.getMessage());
                    uiError.printStackTrace();
                    showStatus("Lỗi xử lý dữ liệu game: " + uiError.getMessage(), true);
                }
            });
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
    }

    public void setRoom(RoomDTO room) {
        this.currentRoom = room;
    }

    public void initializeGame(GameStateUpdate initialState) {
        lastChatPhase = null;
        updateGameState(initialState);
    }

    private void updateGameState(GameStateUpdate update) {
        this.currentGameState = update;
        
        // Update phase info
        GamePhase phase = update.getPhase();
        phaseLabel.setText("Pha: " + phase.getDisplayName());
        roundLabel.setText("Vòng: " + update.getRoundNumber());
        
        // Timer: chỉ hiển thị theo server (tránh lệch / đếm kép với tick server)
        timeRemaining = update.getTimeRemaining();
        updateTimer();
        
        // Find my role
        if (update.getPlayers() != null && currentUser != null) {
            for (PlayerDTO player : update.getPlayers()) {
                if (player.getUserId() == currentUser.getId()) {
                    myRole = player.getRole();
                    if (myRole != null) {
                        myRoleLabel.setText(myRole.getDisplayName());
                    }
                    break;
                }
            }
        }
        
        // Update players display
        updatePlayersDisplay(update.getPlayers());
        
        // Handle different phases
        handlePhase(phase, update);
        updateTurnInfo(phase, update);
        maybeLogPhaseToChat(phase, update);
        
        // Handle game end
        if (phase == GamePhase.ENDED) {
            handleGameEnd(update);
        }
    }

    private void updateTimer() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("Thời gian: %02d:%02d", minutes, seconds));
    }

    private void updatePlayersDisplay(List<PlayerDTO> players) {
        playerPane.getChildren().clear();
        
        if (players == null) return;
        
        for (PlayerDTO player : players) {
            VBox playerCard = createPlayerCard(player);
            playerPane.getChildren().add(playerCard);
        }
    }

    private VBox createPlayerCard(PlayerDTO player) {
        VBox card = new VBox(5);
        card.getStyleClass().add("player-card");
        if (player.isAlive()) {
            card.getStyleClass().add("alive");
        } else {
            card.getStyleClass().add("dead");
        }

        // Avatar (simple vector "image"): circle + first letter
        String dn = player.getDisplayName() != null ? player.getDisplayName().trim() : "?";
        String initial = dn.isEmpty() ? "?" : dn.substring(0, 1).toUpperCase();
        Circle avatar = new Circle(16);
        avatar.getStyleClass().add("avatar-circle");
        Text avatarText = new Text(initial);
        avatarText.getStyleClass().add("avatar-text");
        StackPane avatarPane = new StackPane(avatar, avatarText);

        Label nameLabel = new Label(player.getDisplayName());
        nameLabel.getStyleClass().add("player-name");
        
        Label statusLabel = new Label(player.isAlive() ? "Sống" : "Chết");
        statusLabel.getStyleClass().add("player-status");
        if (player.isAlive()) {
             statusLabel.setStyle("-fx-text-fill: #55efc4;");
        } else {
             statusLabel.setStyle("-fx-text-fill: #ff7675;");
        }
        
        card.getChildren().addAll(avatarPane, nameLabel, statusLabel);
        
        // Show role if player is dead or if it's my role
        if (!player.isAlive() && player.getRole() != null) {
            Label roleLabel = new Label(player.getRole().getDisplayName());
            roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #b2bec3;");
            card.getChildren().add(roleLabel);
        } else if (player.getUserId() == currentUser.getId() && player.getRole() != null) {
            Label roleLabel = new Label(player.getRole().getDisplayName());
            roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffd700; -fx-font-weight: bold;");
            card.getChildren().add(roleLabel);
        }
        
        return card;
    }

    private void handlePhase(GamePhase phase, GameStateUpdate update) {
        actionPanel.setVisible(false);
        voteResultPanel.setVisible(false);
        
        switch (phase) {
            case WAITING:
                phaseDescriptionLabel.setText("Đang chờ trong phòng...");
                break;
            case STARTING:
                phaseDescriptionLabel.setText("Game đang khởi động...");
                break;
                
            case NIGHT_WOLF:
                phaseDescriptionLabel.setText("Đêm - Ma Sói chọn người để giết");
                if (myRole == Role.WEREWOLF && update.isCanAct()) {
                    showActionPanel("Chọn người để giết:", "KILL", update.getPlayers());
                }
                break;
                
            case NIGHT_GUARD:
                phaseDescriptionLabel.setText("Đêm - Bảo vệ chọn người bảo vệ");
                if (myRole == Role.GUARD && update.isCanAct()) {
                    showActionPanel("Chọn người để bảo vệ:", "GUARD_PROTECT", update.getPlayers());
                }
                break;
                
            case NIGHT_WITCH:
                phaseDescriptionLabel.setText("Đêm - Phù thủy hành động (cứu, độc, câm chat)");
                if (myRole == Role.WITCH && update.isCanAct()) {
                    Integer killedId = update.getKilledPlayerId();
                    if (killedId != null) {
                         String killedName = findPlayerName(killedId, update.getPlayers());
                         showWitchActionPanel(killedName);
                    } else {
                         showWitchActionPanel(null);
                    }
                }
                break;

            case NIGHT_SEER:
                phaseDescriptionLabel.setText("Đêm - Tiên Tri soi người");
                if (myRole == Role.SEER && update.isCanAct()) {
                    showActionPanel("Chọn người để soi:", "SEER_CHECK", update.getPlayers());
                }
                break;
                
            case DAY_ANNOUNCE:
                phaseDescriptionLabel.setText("Ngày - Công bố người chết");
                if (update.getKilledPlayerId() != null) {
                    String killedName = findPlayerName(update.getKilledPlayerId(), update.getPlayers());
                    phaseDescriptionLabel.setText("Người chết: " + killedName);
                } else {
                    phaseDescriptionLabel.setText("Không ai chết đêm qua / lượt vừa rồi");
                }
                break;

            case DAY_HUNTER:
                phaseDescriptionLabel.setText("Thợ Săn bị giết — chọn người kéo theo!");
                if (myRole == Role.HUNTER && update.isCanAct()) {
                    showActionPanel("Chọn người để kéo theo:", "HUNTER_SHOOT", update.getPlayers());
                }
                break;
                
            case DAY_CHAT:
                phaseDescriptionLabel.setText("Ngày - Thảo luận");
                break;
                
            case DAY_VOTE:
                phaseDescriptionLabel.setText("Ngày - Bỏ phiếu treo cổ");
                // Nếu còn sống thì ưu tiên panel vote action, tránh chồng với voteResultPanel.
                if (update.isCanAct()) {
                    showActionPanel("Chọn người để bỏ phiếu (có thể đổi vote):", "VOTE", update.getPlayers());
                } else if (update.getVotes() != null && !update.getVotes().isEmpty()) {
                    // Người đã chết/chưa tới lượt thì xem kết quả realtime.
                    showVoteResults(update);
                }
                break;
                
            case ENDED:
                // Handled in handleGameEnd
                break;
        }
    }

    private void showActionPanel(String description, String action, List<PlayerDTO> players) {
        actionDescriptionLabel.setText(description);
        actionPanel.setVisible(true);
        voteResultPanel.setVisible(false);
        
        // Giữ selection hiện tại khi server push update mỗi giây
        PlayerDTO selectedBefore = targetList.getSelectionModel().getSelectedItem();
        Integer selectedUserId = selectedBefore != null ? selectedBefore.getUserId() : null;

        // Filter players based on action
        List<PlayerDTO> filteredTargets = new ArrayList<>();
        for (PlayerDTO player : players) {
            if (action.equals("KILL")) {
                // Wolves can only kill alive non-wolves
                if (player.isAlive() && player.getRole() != Role.WEREWOLF &&
                    player.getUserId() != currentUser.getId()) {
                    filteredTargets.add(player);
                }
            } else if (action.equals("SEER_CHECK")) {
                // Seer can check anyone alive except self
                if (player.isAlive() && player.getUserId() != currentUser.getId()) {
                    filteredTargets.add(player);
                }
            } else if (action.equals("VOTE")) {
                // Vote for anyone alive except self
                if (player.isAlive() && player.getUserId() != currentUser.getId()) {
                    filteredTargets.add(player);
                }
            } else if (action.equals("GUARD_PROTECT")) {
                // Guard can protect anyone alive (including self)
                if (player.isAlive()) {
                    filteredTargets.add(player);
                }
            } else if (action.equals("HUNTER_SHOOT")) {
                // Hunter can drag any alive player except self
                if (player.isAlive() && player.getUserId() != currentUser.getId()) {
                    filteredTargets.add(player);
                }
            }
        }

        boolean needsRefresh = targetList.getItems().size() != filteredTargets.size();
        if (!needsRefresh) {
            for (int i = 0; i < filteredTargets.size(); i++) {
                if (targetList.getItems().get(i).getUserId() != filteredTargets.get(i).getUserId()) {
                    needsRefresh = true;
                    break;
                }
            }
        }
        if (needsRefresh) {
            targetList.getItems().setAll(filteredTargets);
        }

        if (selectedUserId != null) {
            for (PlayerDTO candidate : targetList.getItems()) {
                if (candidate.getUserId() == selectedUserId) {
                    targetList.getSelectionModel().select(candidate);
                    break;
                }
            }
        }
        
        submitActionButton.setOnAction(e -> {
            PlayerDTO selected = targetList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                GameActionRequest request = new GameActionRequest(action, selected.getUserId());
                networkClient.sendMessage(request);
                actionPanel.setVisible(false);
                showStatus("Đã gửi hành động: " + action, false);
            }
        });
    }

    private void showVoteResults(GameStateUpdate update) {
        actionPanel.setVisible(false);
        voteResultPanel.setVisible(true);
        StringBuilder result = new StringBuilder();
        
        if (update.getVotes() != null) {
            for (Map.Entry<Integer, Integer> entry : update.getVotes().entrySet()) {
                String playerName = findPlayerName(entry.getKey(), update.getPlayers());
                result.append(playerName).append(": ").append(entry.getValue()).append(" phiếu\n");
            }
        }
        
        voteResultLabel.setText(result.toString());
    }

    private void updateTurnInfo(GamePhase phase, GameStateUpdate update) {
        if (turnInfoLabel == null) {
            return;
        }

        String turnText;
        switch (phase) {
            case WAITING:
                turnText = "Đến lượt: Chờ chủ phòng bắt đầu game";
                break;
            case STARTING:
                turnText = "Đến lượt: Hệ thống đang khởi động ván";
                break;
            case NIGHT_WOLF:
                turnText = update.isCanAct() ? "Đến lượt: Bạn (Ma Sói)" : "Đến lượt: Ma Sói";
                break;
            case NIGHT_GUARD:
                turnText = update.isCanAct() ? "Đến lượt: Bạn (Bảo vệ)" : "Đến lượt: Bảo vệ";
                break;
            case NIGHT_WITCH:
                turnText = update.isCanAct() ? "Đến lượt: Bạn (Phù thủy)" : "Đến lượt: Phù thủy";
                break;
            case NIGHT_SEER:
                turnText = update.isCanAct() ? "Đến lượt: Bạn (Tiên tri)" : "Đến lượt: Tiên tri";
                break;
            case DAY_ANNOUNCE:
                turnText = "Đến lượt: Hệ thống công bố kết quả";
                break;
            case DAY_HUNTER:
                turnText = update.isCanAct() ? "Đến lượt: Bạn (Thợ Săn — báo thù!)" : "Đến lượt: Thợ Săn chọn người kéo theo";
                break;
            case DAY_CHAT:
                turnText = "Đến lượt: Tất cả người sống thảo luận";
                break;
            case DAY_VOTE:
                turnText = update.isCanAct() ? "Đến lượt: Bạn bỏ phiếu" : "Đang chờ kết quả vote";
                break;
            case ENDED:
                turnText = "Đến lượt: Ván đã kết thúc";
                break;
            default:
                turnText = "Đến lượt: Đang cập nhật...";
                break;
        }

        turnInfoLabel.setText(turnText);
        if (update.isCanAct()) {
            turnInfoLabel.getStyleClass().removeAll("turn-info-label", "turn-info-your-turn");
            turnInfoLabel.getStyleClass().addAll("turn-info-label", "turn-info-your-turn");
        } else {
            turnInfoLabel.getStyleClass().removeAll("turn-info-your-turn");
            if (!turnInfoLabel.getStyleClass().contains("turn-info-label")) {
                turnInfoLabel.getStyleClass().add("turn-info-label");
            }
        }
    }

    private void maybeLogPhaseToChat(GamePhase phase, GameStateUpdate update) {
        if (chatArea == null || phase == null) {
            return;
        }
        if (phase == lastChatPhase) {
            return;
        }
        lastChatPhase = phase;
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String detail = buildTurnDetailForChat(phase, update);
        chatArea.appendText(String.format("[%s] [Lượt] Vòng %d — %s — %s\n",
            time, update.getRoundNumber(), phase.getDisplayName(), detail));
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private String buildTurnDetailForChat(GamePhase phase, GameStateUpdate update) {
        switch (phase) {
            case WAITING:
                return "Chờ trong phòng";
            case STARTING:
                return "Khởi động ván";
            case NIGHT_WOLF:
                return update.isCanAct() ? "Tới lượt bạn (Ma Sói)" : "Tới lượt Ma Sói";
            case NIGHT_GUARD:
                return update.isCanAct() ? "Tới lượt bạn (Bảo vệ)" : "Tới lượt Bảo vệ";
            case NIGHT_WITCH:
                return update.isCanAct() ? "Tới lượt bạn (Phù thủy)" : "Tới lượt Phù thủy";
            case NIGHT_SEER:
                return update.isCanAct() ? "Tới lượt bạn (Tiên tri)" : "Tới lượt Tiên tri";
            case DAY_ANNOUNCE:
                return "Hệ thống công bố kết quả";
            case DAY_HUNTER:
                return update.isCanAct() ? "Tới lượt bạn (Thợ Săn — báo thù)" : "Thợ Săn đang chọn người kéo theo";
            case DAY_CHAT:
                return "Mọi người sống được thảo luận";
            case DAY_VOTE:
                return update.isCanAct() ? "Tới lượt bạn bỏ phiếu" : "Chờ kết quả vote";
            case ENDED:
                return "Ván kết thúc";
            default:
                return "";
        }
    }

    private void showWitchActionPanel(String killedName) {
        actionPanel.setVisible(true);
        voteResultPanel.setVisible(false);
        actionDescriptionLabel.setText(killedName != null ? 
            "Người chết: " + killedName + ". Bạn muốn cứu không?" : 
            "Không ai chết đêm nay.");

        targetList.getItems().clear();
        
        // Simplified Map: Use target list for KILL, specific button for SAVE
        // BUT current UI structure is rigid (Description + List + Submit).
        // Reuse: 
        // If killedName != null: Description asks "Save?". List contains "YES (SAVE)" and then other players to KILL?
        
        // Better: Just use list.
        if (killedName != null) {
            // Add a "Dummy" player representing the Save action? Or just button?
            // Reusing existing submitActionButton is tricky.
            
            // Let's modify actionDescriptionLabel to include instructions
            actionDescriptionLabel.setText("Người chết: " + killedName + ". Chat 'SAVE' để cứu, hoặc chọn người dưới để Độc/Câm chat.");
        } else {
            actionDescriptionLabel.setText("Chọn người để dùng thuốc độc hoặc câm chat:");
        }
        
        for (PlayerDTO player : currentGameState.getPlayers()) {
            if (player.isAlive() && player.getUserId() != currentUser.getId()) {
                targetList.getItems().add(player);
            }
        }
        
        // Override button action
        submitActionButton.setOnAction(e -> {
             PlayerDTO selected = targetList.getSelectionModel().getSelectedItem();
             if (selected != null) {
                 ChoiceDialog<String> choiceDialog = new ChoiceDialog<>("Độc", "Độc", "Câm chat");
                 choiceDialog.setTitle("Chọn kỹ năng");
                 choiceDialog.setHeaderText("Phù thủy chọn kỹ năng cho " + selected.getDisplayName());
                 choiceDialog.setContentText("Kỹ năng:");
                 choiceDialog.showAndWait().ifPresent(choice -> {
                     String action = "Độc".equals(choice) ? "WITCH_KILL" : "WITCH_SILENCE";
                     GameActionRequest request = new GameActionRequest(action, selected.getUserId());
                     networkClient.sendMessage(request);
                 });
                 actionPanel.setVisible(false);
             }
        });
        
        // Note: Real Witch UI needs 2 buttons (Save/Kill). 
        // For MVP, we might rely on text commands or simpler logic.
        // Let's implement chat commands in handleSendChat to support "SAVE" if it's Witch phase?
        // Or update showActionPanel logic to be flexible.
    }

    private String findPlayerName(int userId, List<PlayerDTO> players) {
        if (players == null) return "Unknown";
        for (PlayerDTO player : players) {
            if (player.getUserId() == userId) {
                return player.getDisplayName();
            }
        }
        return "Unknown";
    }

    private void handleGameEnd(GameStateUpdate update) {
        gameEndPanel.setVisible(true);
        actionPanel.setVisible(false);
        voteResultPanel.setVisible(false);
        
        String winner = update.getWinnerTeam();
        if ("VILLAGERS".equals(winner)) {
            winnerLabel.setText("Dân làng thắng!");
            winnerLabel.setStyle("-fx-text-fill: green; -fx-font-size: 18px; -fx-font-weight: bold;");
        } else if ("WEREWOLVES".equals(winner)) {
            winnerLabel.setText("Ma Sói thắng!");
            winnerLabel.setStyle("-fx-text-fill: red; -fx-font-size: 18px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleSubmitAction() {
        // Handled in showActionPanel
    }

    @FXML
    private void handleSendChat() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;
        
        // Hacky: Witch SAVE command via chat
        if (myRole == Role.WITCH && "SAVE".equalsIgnoreCase(message) && currentGameState.getPhase() == GamePhase.NIGHT_WITCH) {
             GameActionRequest request = new GameActionRequest("SAVE", 0); // TargetId ignored for save
             networkClient.sendMessage(request);
             chatInput.clear();
             showStatus("Đã dùng bình cứu!", false);
             actionPanel.setVisible(false);
             return;
        }

        ChatRequest request = new ChatRequest(message);
        networkClient.sendMessage(request);
        chatInput.clear();
    }

    @FXML
    private void handleBackToMain() {
        loadMainScreen();
    }

    private void handleServerMessage(Message message) {
        switch (message.getType()) {
            case "GAME_STATE_UPDATE":
                updateGameState((GameStateUpdate) message);
                break;
            case "USER_PROGRESS_UPDATE":
                handleUserProgressUpdate((UserProgressUpdate) message);
                break;
            case "CHAT_MESSAGE":
                handleChatMessage((ChatMessage) message);
                break;
            case "SYSTEM_MESSAGE":
                handleSystemMessage((SystemMessage) message);
                break;
            case "ERROR_RESPONSE":
                handleErrorResponse((ErrorResponse) message);
                break;
        }
    }

    private void handleUserProgressUpdate(UserProgressUpdate update) {
        if (update == null || update.getUser() == null) return;
        this.currentUser = update.getUser();
        // Khi nhận progression mới, cập nhật lại label hiển thị của màn chính sau khi user quay về.
        showStatus("Đã cập nhật rank/XP/coin!", false);
    }

    private void handleChatMessage(ChatMessage message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(message.getTimestamp()));
        String displayText = String.format("[%s] %s: %s\n", 
            time, message.getDisplayName(), message.getContent());
        chatArea.appendText(displayText);
    }

    private void handleSystemMessage(SystemMessage message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String displayText = String.format("[%s] [Hệ thống] %s\n", time, message.getContent());
        chatArea.appendText(displayText);
        showStatus(message.getContent(), false);
    }

    private void handleErrorResponse(ErrorResponse response) {
        showStatus("Lỗi: " + response.getErrorMessage(), true);
    }

    private void loadMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            MainScreenController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            controller.loadRoomList();

            Scene mainScene = new Scene(root, 800, 600);
            applyTheme(mainScene);
            stage.setScene(mainScene);
            stage.setTitle("Werewolf Game - Danh sách phòng");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        if (isError) {
            statusLabel.getStyleClass().add("status-error");
        } else {
            statusLabel.getStyleClass().add("status-success");
        }
    }
}