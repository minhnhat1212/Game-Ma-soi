package com.werewolf.shared.dto;

public class RoomUpdate extends Message {
    private RoomDTO room;

    public RoomUpdate() {
        this.type = "ROOM_UPDATE";
    }

    public RoomUpdate(RoomDTO room) {
        this();
        this.room = room;
    }

    public RoomDTO getRoom() {
        return room;
    }

    public void setRoom(RoomDTO room) {
        this.room = room;
    }
}
