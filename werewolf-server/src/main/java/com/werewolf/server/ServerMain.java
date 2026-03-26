package com.werewolf.server;

import com.werewolf.server.service.AuthService;
import com.werewolf.server.service.GameEngine;
import com.werewolf.server.service.RoomService;
import com.werewolf.server.network.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server main class
 * Sử dụng TCP Socket (không dùng WebSocket để đơn giản hóa)
 * 
 * Lý do chọn TCP Socket thay vì WebSocket:
 * - Đơn giản hơn, không cần HTTP upgrade
 * - Phù hợp với ứng dụng desktop Java
 * - Dễ debug và test
 * - Đủ cho yêu cầu đồ án
 */
public class ServerMain {
    private static final int PORT = 8888;
    private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
    private final Map<Integer, GameEngine> gameEngines = new ConcurrentHashMap<>();
    private final AuthService authService = new AuthService();
    private final RoomService roomService = new RoomService();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[ServerMain] Build with crash-guard logging enabled.");
            System.out.println("Werewolf Server đang chạy trên port " + PORT);
            System.out.println("Chờ kết nối từ client...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getRemoteSocketAddress().toString();
                System.out.println("========================================");
                System.out.println("✓ Client kết nối: " + clientAddress);
                System.out.println("========================================");
                
                ClientHandler handler = new ClientHandler(
                    clientSocket,
                    authService,
                    roomService,
                    gameEngines
                );
                clientThreadPool.submit(handler);
            }
        } catch (IOException e) {
            System.err.println("Lỗi server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ServerMain server = new ServerMain();
        server.start();
    }
}
