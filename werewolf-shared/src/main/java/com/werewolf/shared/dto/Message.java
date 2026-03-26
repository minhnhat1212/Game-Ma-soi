package com.werewolf.shared.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class cho tất cả các message giữa client và server
 * Sử dụng Jackson polymorphism để deserialize đúng loại message
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    // Client -> Server
    @JsonSubTypes.Type(value = LoginRequest.class, name = "LOGIN_REQUEST"),
    @JsonSubTypes.Type(value = RegisterRequest.class, name = "REGISTER_REQUEST"),
    @JsonSubTypes.Type(value = CreateRoomRequest.class, name = "CREATE_ROOM_REQUEST"),
    @JsonSubTypes.Type(value = JoinRoomRequest.class, name = "JOIN_ROOM_REQUEST"),
    @JsonSubTypes.Type(value = LeaveRoomRequest.class, name = "LEAVE_ROOM_REQUEST"),
    @JsonSubTypes.Type(value = ReadyRequest.class, name = "READY_REQUEST"),
    @JsonSubTypes.Type(value = StartGameRequest.class, name = "START_GAME_REQUEST"),
    @JsonSubTypes.Type(value = AddBotRequest.class, name = "ADD_BOT_REQUEST"),
    @JsonSubTypes.Type(value = GameActionRequest.class, name = "GAME_ACTION_REQUEST"),
    @JsonSubTypes.Type(value = ChatRequest.class, name = "CHAT_REQUEST"),
    @JsonSubTypes.Type(value = GetRoomListRequest.class, name = "GET_ROOM_LIST_REQUEST"),
    
    // Server -> Client
    @JsonSubTypes.Type(value = LoginResponse.class, name = "LOGIN_RESPONSE"),
    @JsonSubTypes.Type(value = ErrorResponse.class, name = "ERROR_RESPONSE"),
    @JsonSubTypes.Type(value = RoomListResponse.class, name = "ROOM_LIST_RESPONSE"),
    @JsonSubTypes.Type(value = RoomUpdate.class, name = "ROOM_UPDATE"),
    @JsonSubTypes.Type(value = GameStateUpdate.class, name = "GAME_STATE_UPDATE"),
    @JsonSubTypes.Type(value = ChatMessage.class, name = "CHAT_MESSAGE"),
    @JsonSubTypes.Type(value = SystemMessage.class, name = "SYSTEM_MESSAGE"),
    @JsonSubTypes.Type(value = UserProgressUpdate.class, name = "USER_PROGRESS_UPDATE")
})
public abstract class Message {
    protected String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
