package com.werewolf.shared.dto;

public class ReadyRequest extends Message {
    private boolean ready;

    public ReadyRequest() {
        this.type = "READY_REQUEST";
    }

    public ReadyRequest(boolean ready) {
        this();
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
