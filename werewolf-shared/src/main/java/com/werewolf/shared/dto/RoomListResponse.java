package com.werewolf.shared.dto;

import java.util.List;

public class RoomListResponse extends Message {
    private List<RoomDTO> rooms;

    public RoomListResponse() {
        this.type = "ROOM_LIST_RESPONSE";
    }

    public RoomListResponse(List<RoomDTO> rooms) {
        this();
        this.rooms = rooms;
    }

    public List<RoomDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomDTO> rooms) {
        this.rooms = rooms;
    }
}
