package com.werewolf.client;

import com.werewolf.client.network.NetworkClient;
import com.werewolf.client.ui.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Client main class với JavaFX
 */
public class ClientMain extends Application {
    private NetworkClient networkClient;
    private Stage primaryStage;
    private com.werewolf.shared.dto.UserDTO currentUser;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("[Client] Uncaught exception on thread " + thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        });
        System.out.println("Werewolf Client đang khởi động...");
        
        // Kết nối với server trước
        connectToServer();
        
        // Load Login UI
        loadLoginScreen();
        primaryStage.setOnCloseRequest(event -> {
            if (networkClient != null && networkClient.isConnected()) {
                networkClient.disconnect();
            }
        });
    }
    
    private void connectToServer() {
        networkClient = new NetworkClient();
        
        // Thử kết nối
        System.out.println("[Client] Đang kết nối với server localhost:8888...");
        boolean connected = networkClient.connect();
        
        if (connected) {
            System.out.println("[Client] ✓ Kết nối thành công với server!");
        } else {
            System.out.println("[Client] ✗ Không thể kết nối với server!");
            System.err.println("Lưu ý: Đảm bảo server đã chạy trên port 8888");
        }
    }
    
    private void loadLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            LoginController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            controller.setStage(primaryStage);
            controller.setOnLoginSuccess((user) -> {
                System.out.println("[Client] Đăng nhập thành công! Chuyển sang màn hình chính...");
                currentUser = user;
                showMainScreen();
            });
            
            Scene scene = new Scene(root, 500, 450);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setTitle("Werewolf Game - Đăng nhập");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
            
            System.out.println("Login UI đã hiển thị.");
        } catch (Exception e) {
            System.err.println("Lỗi load Login UI: " + e.getMessage());
            e.printStackTrace();
            // Fallback: hiển thị UI đơn giản
            showSimpleUI();
        }
    }
    
    private void showMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            com.werewolf.client.ui.MainScreenController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            controller.setStage(primaryStage);
            if (currentUser != null) {
                controller.setCurrentUser(currentUser);
            }
            controller.loadRoomList();
            
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setTitle("Werewolf Game - Danh sách phòng");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            
            System.out.println("[Client] Main screen đã hiển thị.");
        } catch (Exception e) {
            System.err.println("Lỗi load Main UI: " + e.getMessage());
            e.printStackTrace();
            showSimpleUI();
        }
    }
    
    private void showSimpleUI() {
        // Fallback UI nếu không load được FXML
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
        javafx.scene.control.Label label = new javafx.scene.control.Label(
            "Lỗi load UI. Vui lòng kiểm tra file FXML.\n" +
            "Client đã kết nối với server: " + (networkClient != null && networkClient.isConnected())
        );
        root.getChildren().add(label);
        root.setStyle("-fx-padding: 20px;");
        
        Scene scene = new Scene(root, 400, 200);
        primaryStage.setTitle("Werewolf Game - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Lỗi khởi động JavaFX: " + e.getMessage());
            System.err.println("Nếu gặp lỗi JavaFX, có thể do thiếu JavaFX runtime.");
            System.err.println("Hãy đảm bảo đã cài đặt JavaFX hoặc dùng Java 17+ với JavaFX built-in.");
            e.printStackTrace();
        }
    }
}
