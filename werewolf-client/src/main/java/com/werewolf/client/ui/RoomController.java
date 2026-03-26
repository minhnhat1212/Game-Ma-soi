package com.werewolf.client.ui;

import com.werewolf.client.network.NetworkClient;
import com.werewolf.shared.dto.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Controller cho màn hình phòng - chờ người chơi và ready
 */
public class RoomController {
    @FXML
    private Label roomNameLabel;
    @FXML
    private Label roomInfoLabel;
    @FXML
    private Button leaveButton;
    @FXML
    private ListView<PlayerDTO> playerList;
    @FXML
    private Button readyButton;
    @FXML
    private Button addBotButton;
    @FXML
    private Button startButton;
    @FXML
    private Label statusLabel;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField chatInput;

    private NetworkClient networkClient;
    private Stage stage;
    private UserDTO currentUser;
    private RoomDTO currentRoom;
    private boolean isReady = false;
    private boolean isHost = false;

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
    }

    public void initialize() {
        // Setup player list cell factory
        playerList.setCellFactory(listView -> new ListCell<PlayerDTO>() {
            @Override
            protected void updateItem(PlayerDTO player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    Label nameLabel = new Label(player.getDisplayName());
                    nameLabel.getStyleClass().add("player-name");
                    
                    if (player.isHost()) {
                        Label hostLabel = new Label("[Chủ phòng]");
                        hostLabel.getStyleClass().add("host-label");
                        hbox.getChildren().add(hostLabel);
                    }
                    
                    if (player.isReady()) {
                        Label readyLabel = new Label("✓ Sẵn sàng");
                        readyLabel.getStyleClass().add("ready-label");
                        hbox.getChildren().add(readyLabel);
                    }
                    
                    hbox.getChildren().add(nameLabel);
                    setGraphic(hbox);
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
                    System.err.println("[RoomController] Lỗi xử lý message: " + uiError.getMessage());
                    uiError.printStackTrace();
                    showStatus("Lỗi xử lý dữ liệu phòng: " + uiError.getMessage(), true);
                    if (isHost) {
                        startButton.setDisable(false);
                        addBotButton.setDisable(false);
                    }
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
        updateRoomInfo();
    }

    private void updateRoomInfo() {
        if (currentRoom == null) return;
        
        roomNameLabel.setText("Phòng: " + currentRoom.getName());
        roomInfoLabel.setText(
            "Người chơi: " + currentRoom.getCurrentPlayers() + "/" + currentRoom.getMaxPlayers()
        );
        
        // Check if current user is host
        isHost = currentUser != null && currentUser.getId() == currentRoom.getHostUserId();
        startButton.setVisible(isHost);
        addBotButton.setVisible(isHost);
        
        // Disable add bot button if room is full
        if (isHost) {
            addBotButton.setDisable(currentRoom.getCurrentPlayers() >= currentRoom.getMaxPlayers());
        }
        
        // Update player list
        if (currentRoom.getPlayers() != null) {
            playerList.getItems().clear();
            playerList.getItems().addAll(currentRoom.getPlayers());
            
            // Check if current user is ready
            for (PlayerDTO player : currentRoom.getPlayers()) {
                if (player.getUserId() == currentUser.getId()) {
                    isReady = player.isReady();
                    readyButton.setText(isReady ? "Hủy sẵn sàng" : "Sẵn sàng");
                    break;
                }
            }
        }
        
        // Update start button state
        if (isHost) {
            boolean allReady = currentRoom.getPlayers() != null && 
                             currentRoom.getPlayers().size() >= 4 &&
                             currentRoom.getPlayers().stream().allMatch(PlayerDTO::isReady);
            startButton.setDisable(!allReady);
        }
    }

    @FXML
    private void handleReady() {
        isReady = !isReady;
        ReadyRequest request = new ReadyRequest(isReady);
        networkClient.sendMessage(request);
        readyButton.setDisable(true);
    }

    @FXML
    private void handleAddBot() {
        if (!isHost) return;
        
        // Thêm 1 bot mỗi lần click
        AddBotRequest request = new AddBotRequest(1);
        networkClient.sendMessage(request);
        addBotButton.setDisable(true);
        showStatus("Đang thêm bot...", false);
    }

    @FXML
    private void handleStartGame() {
        if (!isHost) return;
        
        StartGameRequest request = new StartGameRequest();
        networkClient.sendMessage(request);
        startButton.setDisable(true);
        showStatus("Đang bắt đầu game...", false);
    }

    @FXML
    private void handleLeaveRoom() {
        LeaveRoomRequest request = new LeaveRoomRequest();
        networkClient.sendMessage(request);
        // Quay về màn hình chính
        loadMainScreen();
    }

    @FXML
    private void handleSendChat() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;
        
        ChatRequest request = new ChatRequest(message);
        networkClient.sendMessage(request);
        chatInput.clear();
    }

    private void handleServerMessage(Message message) {
        switch (message.getType()) {
            case "ROOM_UPDATE":
                handleRoomUpdate((RoomUpdate) message);
                break;
            case "CHAT_MESSAGE":
                handleChatMessage((ChatMessage) message);
                break;
            case "SYSTEM_MESSAGE":
                handleSystemMessage((SystemMessage) message);
                break;
            case "GAME_STATE_UPDATE":
                // Game đã bắt đầu, chuyển sang màn hình game
                handleGameStateUpdate((GameStateUpdate) message);
                break;
            case "ERROR_RESPONSE":
                handleErrorResponse((ErrorResponse) message);
                break;
        }
    }

    private void handleRoomUpdate(RoomUpdate update) {
        currentRoom = update.getRoom();
        updateRoomInfo();
        readyButton.setDisable(false);
        if (isHost) {
            addBotButton.setDisable(false);
        }
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

    private void handleGameStateUpdate(GameStateUpdate update) {
        // Game đã bắt đầu, chuyển sang màn hình game
        loadGameScreen(update);
    }

    private void loadGameScreen(GameStateUpdate initialState) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
            Parent root = loader.load();
            
            GameController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            controller.setRoom(currentRoom);
            controller.initializeGame(initialState);
            
            Scene scene = new Scene(root, 1000, 750);
            applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle("Werewolf Game - " + currentRoom.getName());
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Lỗi load màn hình game: " + e.getMessage(), true);
        }
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

    private void handleErrorResponse(ErrorResponse response) {
        showStatus("Lỗi: " + response.getErrorMessage(), true);
        readyButton.setDisable(false);
        if (isHost) {
            startButton.setDisable(false);
            addBotButton.setDisable(false);
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