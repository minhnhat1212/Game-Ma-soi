package com.werewolf.shared.dto;

public class AddBotRequest extends Message {
    private int count;

    public AddBotRequest() {
        this.type = "ADD_BOT_REQUEST";
    }

    public AddBotRequest(int count) {
        this();
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
