package com.werewolf.shared.dto;

public class ChatRequest extends Message {
    private String content;

    public ChatRequest() {
        this.type = "CHAT_REQUEST";
    }

    public ChatRequest(String content) {
        this();
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
