package com.werewolf.shared.dto;

public class SystemMessage extends Message {
    private String content;
    private long timestamp;

    public SystemMessage() {
        this.type = "SYSTEM_MESSAGE";
    }

    public SystemMessage(String content) {
        this();
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
