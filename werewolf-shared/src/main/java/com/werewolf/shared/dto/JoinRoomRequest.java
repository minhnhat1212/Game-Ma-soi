package com.werewolf.shared.dto;

public class JoinRoomRequest extends Message {
    private int roomId;
    private String password; // Optional

    public JoinRoomRequest() {
        this.type = "JOIN_ROOM_REQUEST";
    }

    public JoinRoomRequest(int roomId, String password) {
        this();
        this.roomId = roomId;
        this.password = password;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
