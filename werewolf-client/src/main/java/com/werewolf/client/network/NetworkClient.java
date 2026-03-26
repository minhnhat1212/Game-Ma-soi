package com.werewolf.client.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolf.shared.dto.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Network client để giao tiếp với server
 */
public class NetworkClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectMapper objectMapper;
    private MessageListener messageListener;
    private boolean connected = false;

    public NetworkClient() {
        this.objectMapper = new ObjectMapper();
    }

    public interface MessageListener {
        void onMessage(Message message);
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            // Start listener thread
            Thread listenerThread = new Thread(this::listenForMessages, "werewolf-client-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối server: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi ngắt kết nối: " + e.getMessage());
        }
    }

    public void sendMessage(Message message) {
        if (!connected || out == null) {
            System.err.println("Chưa kết nối server");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            out.println(json);
        } catch (Exception e) {
            System.err.println("Lỗi gửi message: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            String inputLine;
            while (connected && (inputLine = in.readLine()) != null) {
                try {
                    Message message = objectMapper.readValue(inputLine, Message.class);
                    if (messageListener != null) {
                        try {
                            messageListener.onMessage(message);
                        } catch (Throwable callbackError) {
                            System.err.println("Lỗi xử lý message listener: " + callbackError.getMessage());
                            callbackError.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi parse message từ server: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (connected) {
                System.err.println("Mất kết nối: server đã đóng socket.");
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Lỗi đọc message từ server: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
