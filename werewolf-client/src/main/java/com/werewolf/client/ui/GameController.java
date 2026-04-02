package com.werewolf.client.ui;

import com.werewolf.client.network.NetworkClient;
import com.werewolf.shared.dto.*;
import com.werewolf.shared.enums.GamePhase;
import com.werewolf.shared.enums.RankSystem;
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
 * Controller cho man hinh choi game - co day du ky nang dac biet va he thong rank
 */
public class GameController {
    @FXML private Label phaseLabel;
    @FXML private Label timerLabel;
    @FXML private Label roundLabel;
    @FXML private Label phaseDescriptionLabel;
    @FXML private FlowPane playerPane;
    @FXML private VBox actionPanel;
    @FXML private Label actionDescriptionLabel;
    @FXML private ListView<PlayerDTO> targetList;
    @FXML private ListView<PlayerDTO> target2List;
    @FXML private Button submitActionButton;
    @FXML private VBox voteResultPanel;
    @FXML private Label voteResultLabel;
    @FXML private VBox gameEndPanel;
    @FXML private Label winnerLabel;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private Label myRoleLabel;
    @FXML private Label statusLabel;
    @FXML private Label turnInfoLabel;
    @FXML private VBox specialAbilityPanel;
    @FXML private VBox rewardPanel;

    // Ky nang buttons
    @FXML private Button guardSelfBtn;
    @FXML private Button seerIntuitionBtn;
    @FXML private Button wolfFrameBtn;
    @FXML private Button witchReviveBtn;
    @FXML private Button witchSilenceBtn;

    // Reward labels
    @FXML private Label xpGainedLabel;
    @FXML private Label bonusXpLabel;
    @FXML private Label coinsGainedLabel;
    @FXML private Label rankLabel;
    @FXML private ProgressBar xpProgressBar;
    @FXML private Label xpProgressLabel;

    private NetworkClient networkClient;
    private Stage stage;
    private UserDTO currentUser;
    private GameStateUpdate currentGameState;
    private Role myRole;
    private int timeRemaining = 0;
    private GamePhase lastChatPhase = null;

    // Trang thai ky nang
    private boolean seerIntuitionMode = false; // Dang chon 2 nguoi cho Truc giac

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
    }

    public void initialize() {
        actionPanel.managedProperty().bind(actionPanel.visibleProperty());
        voteResultPanel.managedProperty().bind(voteResultPanel.visibleProperty());
        gameEndPanel.managedProperty().bind(gameEndPanel.visibleProperty());

        if (specialAbilityPanel != null) {
            specialAbilityPanel.managedProperty().bind(specialAbilityPanel.visibleProperty());
        }
        if (target2List != null) {
            target2List.managedProperty().bind(target2List.visibleProperty());
        }

        // Cell factory cho targetList
        targetList.setCellFactory(listView -> new ListCell<PlayerDTO>() {
            @Override
            protected void updateItem(PlayerDTO player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) {
                    setText(null);
                } else {
                    String silenced = player.isSilenced() ? " [Cam chat]" : "";
                    setText(player.getDisplayName() + (player.isAlive() ? " (Song)" : " (Chet)") + silenced);
                }
            }
        });

        if (target2List != null) {
            target2List.setCellFactory(listView -> new ListCell<PlayerDTO>() {
                @Override
                protected void updateItem(PlayerDTO player, boolean empty) {
                    super.updateItem(player, empty);
                    if (empty || player == null) {
                        setText(null);
                    } else {
                        setText(player.getDisplayName());
                    }
                }
            });
        }
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
        networkClient.setMessageListener(message -> {
            Platform.runLater(() -> {
                try {
                    handleServerMessage(message);
                } catch (Throwable uiError) {
                    System.err.println("[GameController] Loi xu ly message: " + uiError.getMessage());
                    uiError.printStackTrace();
                    showStatus("Loi xu ly du lieu game: " + uiError.getMessage(), true);
                }
            });
        });
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public void setCurrentUser(UserDTO user) { this.currentUser = user; }
    public void setRoom(RoomDTO room) { /* kept for compatibility */ }

    public void initializeGame(GameStateUpdate initialState) {
        lastChatPhase = null;
        updateGameState(initialState);
    }

    private void updateGameState(GameStateUpdate update) {
        this.currentGameState = update;

        GamePhase phase = update.getPhase();
        phaseLabel.setText("Pha: " + phase.getDisplayName());
        roundLabel.setText("Vong: " + update.getRoundNumber());

        timeRemaining = update.getTimeRemaining();
        updateTimer();

        // Lay vai tro cua ban
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

        updatePlayersDisplay(update.getPlayers());
        handlePhase(phase, update);
        updateTurnInfo(phase, update);
        maybeLogPhaseToChat(phase, update);

        if (phase == GamePhase.ENDED) {
            handleGameEnd(update);
        }
    }

    private void updateTimer() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("Thoi gian: %02d:%02d", minutes, seconds));
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
        card.getStyleClass().add(player.isAlive() ? "alive" : "dead");

        String dn = player.getDisplayName() != null ? player.getDisplayName().trim() : "?";
        String initial = dn.isEmpty() ? "?" : dn.substring(0, 1).toUpperCase();
        Circle avatar = new Circle(16);
        avatar.getStyleClass().add("avatar-circle");
        Text avatarText = new Text(initial);
        avatarText.getStyleClass().add("avatar-text");
        StackPane avatarPane = new StackPane(avatar, avatarText);

        Label nameLabel = new Label(player.getDisplayName());
        nameLabel.getStyleClass().add("player-name");

        Label statusLbl = new Label(player.isAlive() ? "Song" : "Chet");
        statusLbl.getStyleClass().add("player-status");
        statusLbl.setStyle(player.isAlive() ? "-fx-text-fill: #55efc4;" : "-fx-text-fill: #ff7675;");

        card.getChildren().addAll(avatarPane, nameLabel, statusLbl);

        // Icon câm chat
        if (player.isSilenced() && player.isAlive()) {
            Label silenceLabel = new Label("[Cam chat]");
            silenceLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #fd79a8; -fx-font-weight: bold;");
            card.getChildren().add(silenceLabel);
        }

        // Vai tro
        if (!player.isAlive() && player.getRole() != null) {
            Label roleLabel = new Label(player.getRole().getDisplayName());
            roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #b2bec3;");
            card.getChildren().add(roleLabel);
        } else if (currentUser != null && player.getUserId() == currentUser.getId() && player.getRole() != null) {
            Label roleLabel = new Label(player.getRole().getDisplayName());
            roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffd700; -fx-font-weight: bold;");
            card.getChildren().add(roleLabel);
        }

        return card;
    }

    private void handlePhase(GamePhase phase, GameStateUpdate update) {
        actionPanel.setVisible(false);
        voteResultPanel.setVisible(false);
        hideAllAbilityButtons();
        seerIntuitionMode = false;

        switch (phase) {
            case WAITING:
                phaseDescriptionLabel.setText("Dang cho trong phong...");
                break;
            case STARTING:
                phaseDescriptionLabel.setText("Game dang khoi dong...");
                break;

            case NIGHT_WOLF:
                phaseDescriptionLabel.setText("DEM - Ma Soi chon nguoi de giet");
                if (myRole == Role.WEREWOLF && update.isCanAct()) {
                    showActionPanel("Chon nguoi de giet:", "KILL", update.getPlayers());
                    // Frame ability
                    if (update.isWerewolfCanFrame() && !update.isWerewolfFrameUsed()) {
                        showAbilityPanel();
                        setButtonVisible(wolfFrameBtn, true);
                    }
                }
                break;

            case NIGHT_GUARD:
                phaseDescriptionLabel.setText("DEM - Bao ve chon nguoi bao ve");
                if (myRole == Role.GUARD && update.isCanAct()) {
                    showActionPanel("Chon nguoi de bao ve:", "GUARD_PROTECT", update.getPlayers());
                    // Self-protect ability
                    if (update.isGuardCanSelfProtect() && !update.isGuardSelfProtectUsed()) {
                        showAbilityPanel();
                        setButtonVisible(guardSelfBtn, true);
                    }
                }
                break;

            case NIGHT_WITCH:
                phaseDescriptionLabel.setText("DEM - Phu thuy hanh dong");
                if (myRole == Role.WITCH && update.isCanAct()) {
                    Integer killedId = update.getKilledPlayerId();
                    String killedName = killedId != null ? findPlayerName(killedId, update.getPlayers()) : null;
                    showWitchActionPanel(killedName, update);
                }
                break;

            case NIGHT_SEER:
                phaseDescriptionLabel.setText("DEM - Tien Tri soi nguoi");
                if (myRole == Role.SEER && update.isCanAct()) {
                    showActionPanel("Chon nguoi de soi:", "SEER_CHECK", update.getPlayers());
                    // Intuition ability
                    if (update.isSeerHasIntuition() && !update.isSeerIntuitionUsed()) {
                        showAbilityPanel();
                        setButtonVisible(seerIntuitionBtn, true);
                    }
                }
                break;

            case DAY_ANNOUNCE:
                if (update.getKilledPlayerId() != null) {
                    String killedName = findPlayerName(update.getKilledPlayerId(), update.getPlayers());
                    phaseDescriptionLabel.setText("Nguoi chet: " + killedName);
                } else {
                    phaseDescriptionLabel.setText("Khong ai chet dem qua / luot vua roi");
                }
                break;

            case DAY_HUNTER:
                phaseDescriptionLabel.setText("Tho San bi giet — chon nguoi keo theo!");
                if (myRole == Role.HUNTER && update.isCanAct()) {
                    showActionPanel("Chon nguoi de keo theo:", "HUNTER_SHOOT", update.getPlayers());
                }
                break;

            case DAY_CHAT:
                phaseDescriptionLabel.setText("NGAY - Thao luan");
                break;

            case DAY_VOTE:
                phaseDescriptionLabel.setText("NGAY - Bo phieu treo co");
                if (update.isCanAct()) {
                    showActionPanel("Chon nguoi de bo phieu (co the doi vote):", "VOTE", update.getPlayers());
                } else if (update.getVotes() != null && !update.getVotes().isEmpty()) {
                    showVoteResults(update);
                }
                break;

            case ENDED:
                // Handled in handleGameEnd
                break;
        }
    }

    // ===================== ABILITY PANEL =====================

    private void showAbilityPanel() {
        if (specialAbilityPanel != null) {
            specialAbilityPanel.setVisible(true);
        }
    }

    private void hideAllAbilityButtons() {
        if (specialAbilityPanel == null) return;
        specialAbilityPanel.setVisible(false);
        setButtonVisible(guardSelfBtn, false);
        setButtonVisible(seerIntuitionBtn, false);
        setButtonVisible(wolfFrameBtn, false);
        setButtonVisible(witchReviveBtn, false);
        setButtonVisible(witchSilenceBtn, false);
    }

    private void setButtonVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    @FXML
    private void handleGuardSelf() {
        GameActionRequest req = new GameActionRequest("GUARD_SELF", 0);
        networkClient.sendMessage(req);
        setButtonVisible(guardSelfBtn, false);
        showStatus("Da tu bao ve ban than dem nay!", false);
    }

    @FXML
    private void handleSeerIntuition() {
        // Hien thi target2List de chon nguoi thu 2
        if (target2List != null) {
            seerIntuitionMode = true;
            List<PlayerDTO> alivePlayers = new ArrayList<>();
            if (currentGameState != null && currentGameState.getPlayers() != null) {
                for (PlayerDTO p : currentGameState.getPlayers()) {
                    if (p.isAlive() && p.getUserId() != currentUser.getId()) {
                        alivePlayers.add(p);
                    }
                }
            }
            target2List.getItems().setAll(alivePlayers);
            target2List.setVisible(true);
            actionDescriptionLabel.setText("Truc giac: Chon 2 nguoi (dach sach 1 + danh sach 2), roi nhan Xac nhan:");

            // Override submit button
            submitActionButton.setOnAction(e -> {
                PlayerDTO t1 = targetList.getSelectionModel().getSelectedItem();
                PlayerDTO t2 = target2List.getSelectionModel().getSelectedItem();
                if (t1 != null && t2 != null && t1.getUserId() != t2.getUserId()) {
                    GameActionRequest req = new GameActionRequest("SEER_INTUITION", t1.getUserId(), t2.getUserId());
                    networkClient.sendMessage(req);
                    actionPanel.setVisible(false);
                    seerIntuitionMode = false;
                    showStatus("Da dung Truc giac!", false);
                } else {
                    showStatus("Hay chon 2 nguoi khac nhau!", true);
                }
            });
            setButtonVisible(seerIntuitionBtn, false);
        }
    }

    @FXML
    private void handleWolfFrame() {
        PlayerDTO selected = targetList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Frame '" + selected.getDisplayName() + "'? Tien tri se thay ho la Soi!", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Xac nhan Frame");
            confirm.setHeaderText("Ky nang: Frame nguoi");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    GameActionRequest req = new GameActionRequest("WOLF_FRAME", selected.getUserId());
                    networkClient.sendMessage(req);
                    setButtonVisible(wolfFrameBtn, false);
                    showStatus("Da frame " + selected.getDisplayName() + "!", false);
                }
            });
        } else {
            showStatus("Hay chon nguoi muon frame truoc!", true);
        }
    }

    @FXML
    private void handleWitchRevive() {
        // Hoi sinh nguoi bi giet dem nay (killedPlayerId)
        if (currentGameState != null && currentGameState.getKilledPlayerId() != null) {
            int killedId = currentGameState.getKilledPlayerId();
            String name = findPlayerName(killedId, currentGameState.getPlayers());
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hoi sinh tam '" + name + "'? Ho se noi duoc 1 luot roi chet.", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Xac nhan Hoi sinh tam");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    GameActionRequest req = new GameActionRequest("WITCH_REVIVE", killedId);
                    networkClient.sendMessage(req);
                    setButtonVisible(witchReviveBtn, false);
                    showStatus("Da hoi sinh tam " + name + "!", false);
                }
            });
        } else {
            showStatus("Khong co nguoi nao bi giet dem nay!", true);
        }
    }

    @FXML
    private void handleWitchSilence() {
        PlayerDTO selected = targetList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cam chat '" + selected.getDisplayName() + "' trong luot ngay hom nay?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Xac nhan Cam chat");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    GameActionRequest req = new GameActionRequest("WITCH_SILENCE", selected.getUserId());
                    networkClient.sendMessage(req);
                    actionPanel.setVisible(false);
                    showStatus("Da cam chat " + selected.getDisplayName() + "!", false);
                }
            });
        } else {
            showStatus("Hay chon nguoi muon cam chat!", true);
        }
    }

    // ===================== ACTION PANEL =====================

    private void showActionPanel(String description, String action, List<PlayerDTO> players) {
        actionDescriptionLabel.setText(description);
        actionPanel.setVisible(true);
        voteResultPanel.setVisible(false);

        // Reset target2List
        if (target2List != null) {
            target2List.setVisible(false);
        }

        PlayerDTO selectedBefore = targetList.getSelectionModel().getSelectedItem();
        Integer selectedUserId = selectedBefore != null ? selectedBefore.getUserId() : null;

        List<PlayerDTO> filteredTargets = new ArrayList<>();
        if (players != null) {
            for (PlayerDTO player : players) {
                if (action.equals("KILL")) {
                    if (player.isAlive() && player.getRole() != Role.WEREWOLF &&
                        player.getUserId() != currentUser.getId()) {
                        filteredTargets.add(player);
                    }
                } else if (action.equals("SEER_CHECK")) {
                    if (player.isAlive() && player.getUserId() != currentUser.getId()) {
                        filteredTargets.add(player);
                    }
                } else if (action.equals("VOTE")) {
                    if (player.isAlive() && player.getUserId() != currentUser.getId()) {
                        filteredTargets.add(player);
                    }
                } else if (action.equals("GUARD_PROTECT")) {
                    if (player.isAlive()) {
                        filteredTargets.add(player);
                    }
                } else if (action.equals("HUNTER_SHOOT")) {
                    if (player.isAlive() && player.getUserId() != currentUser.getId()) {
                        filteredTargets.add(player);
                    }
                } else {
                    if (player.isAlive()) {
                        filteredTargets.add(player);
                    }
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
                showStatus("Da gui hanh dong: " + action, false);
            }
        });
    }

    private void showWitchActionPanel(String killedName, GameStateUpdate update) {
        actionPanel.setVisible(true);
        voteResultPanel.setVisible(false);
        if (target2List != null) target2List.setVisible(false);

        if (killedName != null) {
            actionDescriptionLabel.setText("Nguoi chet: " + killedName + ". Chat 'SAVE' de cuu, hoac chon nguoi de Doc/Cam chat (Nhan Xac nhan).");
        } else {
            actionDescriptionLabel.setText("Khong ai chet dem nay. Chon nguoi de Doc/Cam chat:");
        }

        List<PlayerDTO> alive = new ArrayList<>();
        if (update.getPlayers() != null) {
            for (PlayerDTO p : update.getPlayers()) {
                if (p.isAlive() && p.getUserId() != currentUser.getId()) {
                    alive.add(p);
                }
            }
        }
        targetList.getItems().setAll(alive);

        submitActionButton.setOnAction(e -> {
            PlayerDTO selected = targetList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ChoiceDialog<String> choiceDialog = new ChoiceDialog<>("Doc", "Doc", "Cam chat");
                choiceDialog.setTitle("Chon ky nang");
                choiceDialog.setHeaderText("Phu thuy chon ky nang cho " + selected.getDisplayName());
                choiceDialog.setContentText("Ky nang:");
                choiceDialog.showAndWait().ifPresent(choice -> {
                    String act = "Doc".equals(choice) ? "WITCH_KILL" : "WITCH_SILENCE";
                    GameActionRequest request = new GameActionRequest(act, selected.getUserId());
                    networkClient.sendMessage(request);
                });
                actionPanel.setVisible(false);
            }
        });

        // Witch special buttons
        showAbilityPanel();
        if (update.isWitchHasMiniRevive() && !update.isWitchMiniReviveUsed() && killedName != null) {
            setButtonVisible(witchReviveBtn, true);
        }
        setButtonVisible(witchSilenceBtn, true); // Cam chat luon hien (dung nhu tuong tac)
    }

    private void showVoteResults(GameStateUpdate update) {
        actionPanel.setVisible(false);
        voteResultPanel.setVisible(true);
        StringBuilder result = new StringBuilder();
        if (update.getVotes() != null) {
            for (Map.Entry<Integer, Integer> entry : update.getVotes().entrySet()) {
                String playerName = findPlayerName(entry.getKey(), update.getPlayers());
                result.append(playerName).append(": ").append(entry.getValue()).append(" phieu\n");
            }
        }
        voteResultLabel.setText(result.toString());
    }

    // ===================== GAME END =====================

    private void handleGameEnd(GameStateUpdate update) {
        gameEndPanel.setVisible(true);
        actionPanel.setVisible(false);
        voteResultPanel.setVisible(false);

        String winner = update.getWinnerTeam();
        if ("VILLAGERS".equals(winner)) {
            winnerLabel.setText("PHE DAN THANG!");
            winnerLabel.setStyle("-fx-text-fill: #00cec9; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else if ("WEREWOLVES".equals(winner)) {
            winnerLabel.setText("MA SOI THANG!");
            winnerLabel.setStyle("-fx-text-fill: #d63031; -fx-font-size: 20px; -fx-font-weight: bold;");
        }
    }

    private void updateRewardPanel(UserProgressUpdate progressUpdate) {
        if (progressUpdate == null || progressUpdate.getUser() == null) return;
        UserDTO updated = progressUpdate.getUser();

        // Tinh xp/coin gain tu GameStateUpdate neu co
        int xpGained = 0, bonusXp = 0, coinsGained = 0;
        if (currentGameState != null) {
            xpGained = currentGameState.getXpGained();
            bonusXp = currentGameState.getBonusXp();
            coinsGained = currentGameState.getCoinsGained();
        }

        if (xpGainedLabel != null) {
            xpGainedLabel.setText("+" + xpGained + " XP");
        }
        if (bonusXpLabel != null) {
            bonusXpLabel.setText("+" + bonusXp + " XP");
        }
        if (coinsGainedLabel != null) {
            coinsGainedLabel.setText("+" + coinsGained);
        }

        // Rank & progress bar
        int level = updated.getLevel();
        int totalXp = updated.getExperience();
        RankSystem rank = RankSystem.fromLevel(level);

        if (rankLabel != null) {
            rankLabel.setText(rank.getIcon() + " " + rank.getDisplayName() + " (Lv." + level + ")");
            rankLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + rank.getColor() + ";");
        }

        if (rank.getMaxLevel() != Integer.MAX_VALUE) {
            int xpInLevel = RankSystem.xpInCurrentLevel(totalXp);
            int xpNeeded = RankSystem.xpRequiredForLevel(level);
            double progress = xpNeeded > 0 ? (double) xpInLevel / xpNeeded : 1.0;
            if (xpProgressBar != null) xpProgressBar.setProgress(progress);
            if (xpProgressLabel != null) xpProgressLabel.setText(xpInLevel + " / " + xpNeeded + " XP");
        } else {
            if (xpProgressBar != null) xpProgressBar.setProgress(1.0);
            if (xpProgressLabel != null) xpProgressLabel.setText("MAX LEVEL");
        }

        if (rewardPanel != null) rewardPanel.setVisible(true);
    }

    // ===================== TURN INFO & CHAT =====================

    private void updateTurnInfo(GamePhase phase, GameStateUpdate update) {
        if (turnInfoLabel == null) return;

        String turnText;
        switch (phase) {
            case WAITING: turnText = "Den luot: Cho chu phong bat dau game"; break;
            case STARTING: turnText = "Den luot: He thong dang khoi dong van"; break;
            case NIGHT_WOLF: turnText = update.isCanAct() ? "Den luot: Ban (Ma Soi)" : "Den luot: Ma Soi"; break;
            case NIGHT_GUARD: turnText = update.isCanAct() ? "Den luot: Ban (Bao ve)" : "Den luot: Bao ve"; break;
            case NIGHT_WITCH: turnText = update.isCanAct() ? "Den luot: Ban (Phu thuy)" : "Den luot: Phu thuy"; break;
            case NIGHT_SEER: turnText = update.isCanAct() ? "Den luot: Ban (Tien tri)" : "Den luot: Tien tri"; break;
            case DAY_ANNOUNCE: turnText = "Den luot: He thong cong bo ket qua"; break;
            case DAY_HUNTER: turnText = update.isCanAct() ? "Den luot: Ban (Tho San - bao thu!)" : "Den luot: Tho San chon nguoi keo theo"; break;
            case DAY_CHAT: turnText = "Den luot: Tat ca nguoi song thao luan"; break;
            case DAY_VOTE: turnText = update.isCanAct() ? "Den luot: Ban bo phieu" : "Dang cho ket qua vote"; break;
            case ENDED: turnText = "Den luot: Van da ket thuc"; break;
            default: turnText = "Den luot: Dang cap nhat..."; break;
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
        if (chatArea == null || phase == null || phase == lastChatPhase) return;
        lastChatPhase = phase;
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        chatArea.appendText(String.format("[%s] [Luot] Vong %d - %s\n",
            time, update.getRoundNumber(), phase.getDisplayName()));
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private String findPlayerName(int userId, List<PlayerDTO> players) {
        if (players == null) return "Unknown";
        for (PlayerDTO player : players) {
            if (player.getUserId() == userId) return player.getDisplayName();
        }
        return "Unknown";
    }

    // ===================== SERVER MESSAGE HANDLERS =====================

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
        updateRewardPanel(update);
        showStatus("Da cap nhat rank/XP/coin!", false);
    }

    private void handleChatMessage(ChatMessage message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(message.getTimestamp()));
        chatArea.appendText(String.format("[%s] %s: %s\n", time, message.getDisplayName(), message.getContent()));
    }

    private void handleSystemMessage(SystemMessage message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        chatArea.appendText(String.format("[%s] [He thong] %s\n", time, message.getContent()));
        showStatus(message.getContent(), false);
    }

    private void handleErrorResponse(ErrorResponse response) {
        showStatus("Loi: " + response.getErrorMessage(), true);
    }

    // ===================== FXML ACTIONS =====================

    @FXML
    private void handleSubmitAction() {
        // Handled by submitActionButton.setOnAction(...)
    }

    @FXML
    private void handleSendChat() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;

        // Witch SAVE command via chat
        if (myRole == Role.WITCH && "SAVE".equalsIgnoreCase(message) &&
            currentGameState != null && currentGameState.getPhase() == GamePhase.NIGHT_WITCH) {
            GameActionRequest request = new GameActionRequest("WITCH_SAVE", 0);
            networkClient.sendMessage(request);
            chatInput.clear();
            showStatus("Da dung binh cuu!", false);
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
            stage.setTitle("Werewolf Game - Danh sach phong");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }
}