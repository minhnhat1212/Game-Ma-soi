package com.werewolf.client.ui;

import com.werewolf.client.network.NetworkClient;
import com.werewolf.shared.dto.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller cho Login/Register UI
 */
public class LoginController {
    @FXML
    private TextField loginUsernameField;
    @FXML
    private PasswordField loginPasswordField;
    @FXML
    private TextField registerUsernameField;
    @FXML
    private PasswordField registerPasswordField;
    @FXML
    private TextField displayNameField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Label statusLabel;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab loginTab;
    @FXML
    private Tab registerTab;

    private NetworkClient networkClient;
    private Stage stage;
    private java.util.function.Consumer<com.werewolf.shared.dto.UserDTO> onLoginSuccess;

    public void initialize() {
        // Set default focus
        Platform.runLater(() -> loginUsernameField.requestFocus());
        
        // Disable register fields initially
        displayNameField.setDisable(true);
        registerButton.setDisable(true);
        
        // Tab change listener
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == loginTab) {
                displayNameField.setDisable(true);
                registerButton.setDisable(true);
                loginButton.setDisable(false);
                Platform.runLater(() -> loginUsernameField.requestFocus());
            } else if (newTab == registerTab) {
                displayNameField.setDisable(false);
                registerButton.setDisable(false);
                loginButton.setDisable(true);
                Platform.runLater(() -> registerUsernameField.requestFocus());
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

    public void setOnLoginSuccess(java.util.function.Consumer<com.werewolf.shared.dto.UserDTO> callback) {
        this.onLoginSuccess = callback;
    }

    @FXML
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Vui lòng điền đầy đủ thông tin!", true);
            return;
        }

        showStatus("Đang đăng nhập...", false);
        loginButton.setDisable(true);

        LoginRequest request = new LoginRequest(username, password);
        networkClient.sendMessage(request);
    }

    @FXML
    private void handleRegister() {
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        String displayName = displayNameField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
            showStatus("Vui lòng điền đầy đủ thông tin!", true);
            return;
        }

        if (username.length() < 3) {
            showStatus("Username phải có ít nhất 3 ký tự!", true);
            return;
        }

        if (password.length() < 4) {
            showStatus("Password phải có ít nhất 4 ký tự!", true);
            return;
        }

        showStatus("Đang đăng ký...", false);
        registerButton.setDisable(true);

        RegisterRequest request = new RegisterRequest(username, password, displayName);
        networkClient.sendMessage(request);
    }

    private void handleServerMessage(Message message) {
        switch (message.getType()) {
            case "LOGIN_RESPONSE":
                handleLoginResponse((LoginResponse) message);
                break;
            case "ERROR_RESPONSE":
                handleErrorResponse((ErrorResponse) message);
                break;
        }
    }

    private void handleLoginResponse(LoginResponse response) {
        if (response.isSuccess()) {
            showStatus("Đăng nhập thành công!", false);
            // Chuyển sang màn hình chính sau 1 giây
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        if (onLoginSuccess != null && response.getUser() != null) {
                            onLoginSuccess.accept(response.getUser());
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showStatus("Đăng nhập thất bại: " + response.getErrorMessage(), true);
            loginButton.setDisable(false);
        }
    }

    private void handleErrorResponse(ErrorResponse response) {
        String currentTab = tabPane.getSelectionModel().getSelectedItem().getText();
        if (currentTab.equals("Đăng nhập")) {
            loginButton.setDisable(false);
        } else {
            registerButton.setDisable(false);
        }
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
