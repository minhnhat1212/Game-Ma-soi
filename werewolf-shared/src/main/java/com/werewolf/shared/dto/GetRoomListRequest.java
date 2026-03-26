package com.werewolf.shared.dto;

public class GetRoomListRequest extends Message {
    private String filter; // "WAITING", "PLAYING", null = all

    public GetRoomListRequest() {
        this.type = "GET_ROOM_LIST_REQUEST";
    }

    public GetRoomListRequest(String filter) {
        this();
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
