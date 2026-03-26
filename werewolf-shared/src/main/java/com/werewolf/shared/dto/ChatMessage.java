package com.werewolf.shared.dto;

import com.werewolf.shared.enums.MessageType;

public class ChatMessage extends Message {
    private int userId;
    private String displayName;
    private String content;
    private MessageType messageType;
    private long timestamp;

    public ChatMessage() {
        this.type = "CHAT_MESSAGE";
    }

    public ChatMessage(int userId, String displayName, String content, MessageType messageType) {
        this();
        this.userId = userId;
        this.displayName = displayName;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
