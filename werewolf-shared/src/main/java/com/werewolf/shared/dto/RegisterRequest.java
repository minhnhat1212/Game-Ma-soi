package com.werewolf.shared.dto;

public class RegisterRequest extends Message {
    private String username;
    private String password;
    private String displayName;

    public RegisterRequest() {
        this.type = "REGISTER_REQUEST";
    }

    public RegisterRequest(String username, String password, String displayName) {
        this();
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
