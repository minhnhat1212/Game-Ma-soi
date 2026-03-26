package com.werewolf.client.ui;

import com.werewolf.client.network.NetworkClient;
import com.werewolf.shared.dto.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Controller cho màn hình chính - danh sách phòng
 */
public class MainScreenController {
    @FXML
    private Label userInfoLabel;
    @FXML
    private Button refreshButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Button createRoomButton;
    @FXML
    private TableView<RoomDTO> roomTable;
    @FXML
    private TableColumn<RoomDTO, Integer> idColumn;
    @FXML
    private TableColumn<RoomDTO, String> nameColumn;
    @FXML
    private TableColumn<RoomDTO, String> hostColumn;
    @FXML
    private TableColumn<RoomDTO, String> playersColumn;
    @FXML
    private TableColumn<RoomDTO, String> statusColumn;
    @FXML
    private TableColumn<RoomDTO, String> actionColumn;
    @FXML
    private Label statusLabel;

    private NetworkClient networkClient;
    private Stage stage;
    private UserDTO currentUser;
    private ObservableList<RoomDTO> rooms;

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
    }

    public void initialize() {
        rooms = FXCollections.observableArrayList();
        roomTable.setItems(rooms);
        
        // Setup table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        hostColumn.setCellValueFactory(cellData -> {
            RoomDTO room = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(room.getHostDisplayName());
        });
        playersColumn.setCellValueFactory(cellData -> {
            RoomDTO room = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                room.getCurrentPlayers() + "/" + room.getMaxPlayers()
            );
        });
        statusColumn.setCellValueFactory(cellData -> {
            RoomDTO room = cellData.getValue();
            String status = room.getStatus();
            if ("WAITING".equals(status)) return new javafx.beans.property.SimpleStringProperty("Chờ người chơi");
            if ("PLAYING".equals(status)) return new javafx.beans.property.SimpleStringProperty("Đang chơi");
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        
        // Action column với button
        actionColumn.setCellFactory(column -> new TableCell<RoomDTO, String>() {
            private final Button joinButton = new Button("Vào phòng");
            {
                // tránh bị add styleClass lặp vô hạn mỗi lần updateItem() chạy
                joinButton.getStyleClass().addAll("success", "small");
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RoomDTO room = getTableView().getItems().get(getIndex());
                    joinButton.setOnAction(e -> handleJoinRoom(room));
                    joinButton.setDisable("PLAYING".equals(room.getStatus()) || 
                                         room.getCurrentPlayers() >= room.getMaxPlayers());
                    setGraphic(joinButton);
                }
            }
        });
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
        
        // Set message listener
        networkClient.setMessageListener(message -> {
            Platform.runLater(() -> handleServerMessage(message));
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
        if (user != null) {
            userInfoLabel.setText(
                "Xin chào, " + user.getDisplayName() +
                " | Rank: " + user.getRank() +
                " | Lv." + user.getLevel() +
                " | XP: " + user.getExperience() +
                " | Coin: " + user.getCoins()
            );
        }
    }

    public void loadRoomList() {
        showStatus("Đang tải danh sách phòng...", false);
        GetRoomListRequest request = new GetRoomListRequest();
        networkClient.sendMessage(request);
    }

    @FXML
    private void handleRefresh() {
        loadRoomList();
    }

    @FXML
    private void handleCreateRoom() {
        Dialog<CreateRoomRequest> dialog = new Dialog<>();
        dialog.setTitle("Tạo phòng mới");
        dialog.setHeaderText("Cấu hình phòng chơi");

        TextField nameField = new TextField();
        nameField.setPromptText("Tên phòng");
        
        Spinner<Integer> maxPlayersSpinner = new Spinner<>(4, 16, 8);
        maxPlayersSpinner.setEditable(true);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu (tùy chọn)");
        
        // Role Configuration
        Spinner<Integer> wolfSpinner = new Spinner<>(1, 5, 2);
        CheckBox seerCheck = new CheckBox("Tiên tri"); seerCheck.setSelected(true);
        CheckBox guardCheck = new CheckBox("Bảo vệ");
        CheckBox witchCheck = new CheckBox("Phù thủy");
        CheckBox hunterCheck = new CheckBox("Thợ săn");

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(
            new Label("Tên phòng:"), nameField,
            new Label("Số người chơi tối đa:"), maxPlayersSpinner,
            new Label("Mật khẩu:"), passwordField,
            new Separator(),
            new Label("Cấu hình vai trò:"),
            new HBox(10, new Label("Sói:"), wolfSpinner),
            seerCheck, guardCheck, witchCheck, hunterCheck
        );
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);

        ButtonType createButtonType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) return null;
                
                CreateRoomRequest req = new CreateRoomRequest(name, 
                                           passwordField.getText().isEmpty() ? null : passwordField.getText(),
                                           maxPlayersSpinner.getValue(), 60);
                
                RoleConfig config = new RoleConfig(wolfSpinner.getValue(), 
                    seerCheck.isSelected(), guardCheck.isSelected(), 
                    witchCheck.isSelected(), hunterCheck.isSelected());
                req.setRoleConfig(config);
                return req;
            }
            return null;
        });

        Optional<CreateRoomRequest> result = dialog.showAndWait();
        result.ifPresent(request -> {
            showStatus("Đang tạo phòng...", false);
            networkClient.sendMessage(request);
        });
    }

    private void handleJoinRoom(RoomDTO room) {
        if (room.isHasPassword()) {
            // Hiển thị dialog nhập mật khẩu
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Nhập mật khẩu");
            passwordDialog.setHeaderText("Phòng này có mật khẩu");
            passwordDialog.setContentText("Mật khẩu:");

            Optional<String> password = passwordDialog.showAndWait();
            if (password.isPresent()) {
                JoinRoomRequest request = new JoinRoomRequest(room.getId(), password.get());
                networkClient.sendMessage(request);
            }
        } else {
            JoinRoomRequest request = new JoinRoomRequest(room.getId(), null);
            networkClient.sendMessage(request);
        }
    }

    @FXML
    private void handleLogout() {
        // TODO: Gửi logout request nếu cần
        // Quay về màn hình login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            LoginController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            controller.setStage(stage);
            controller.setOnLoginSuccess((user) -> {
                // Reload main screen
                try {
                    FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                    Parent mainRoot = mainLoader.load();
                    MainScreenController mainController = mainLoader.getController();
                    mainController.setNetworkClient(networkClient);
                    mainController.setStage(stage);
                    mainController.setCurrentUser(user);
                    mainController.loadRoomList();

                    Scene mainScene = new Scene(mainRoot, 800, 600);
                    applyTheme(mainScene);
                    stage.setScene(mainScene);
                    stage.setTitle("Werewolf Game - Danh sách phòng");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Scene loginScene = new Scene(root, 500, 450);
            applyTheme(loginScene);
            stage.setScene(loginScene);
            stage.setTitle("Werewolf Game - Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleServerMessage(Message message) {
        switch (message.getType()) {
            case "ROOM_LIST_RESPONSE":
                handleRoomListResponse((RoomListResponse) message);
                break;
            case "ROOM_UPDATE":
                handleRoomUpdate((RoomUpdate) message);
                break;
            case "USER_PROGRESS_UPDATE":
                handleUserProgressUpdate((UserProgressUpdate) message);
                break;
            case "ERROR_RESPONSE":
                handleErrorResponse((ErrorResponse) message);
                break;
        }
    }

    private void handleUserProgressUpdate(UserProgressUpdate update) {
        if (update == null || update.getUser() == null) return;
        setCurrentUser(update.getUser());
        showStatus("Đã cập nhật progression!", false);
    }

    private void handleRoomListResponse(RoomListResponse response) {
        rooms.clear();
        rooms.addAll(response.getRooms());
        showStatus("Đã tải " + response.getRooms().size() + " phòng", false);
    }

    private void handleRoomUpdate(RoomUpdate update) {
        // Khi vào phòng thành công, chuyển sang màn hình phòng
        RoomDTO room = update.getRoom();
        if (room != null) {
            showStatus("Đã vào phòng: " + room.getName(), false);
            // Chuyển sang màn hình phòng
            loadRoomScreen(room);
        }
    }

    private void loadRoomScreen(RoomDTO room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/room.fxml"));
            Parent root = loader.load();
            
            RoomController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            controller.setRoom(room);

            Scene roomScene = new Scene(root, 900, 700);
            applyTheme(roomScene);
            stage.setScene(roomScene);
            stage.setTitle("Werewolf Game - Phòng: " + room.getName());
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Lỗi load màn hình phòng: " + e.getMessage(), true);
        }
    }

    private void handleErrorResponse(ErrorResponse response) {
        showStatus("Lỗi: " + response.getErrorMessage(), true);
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