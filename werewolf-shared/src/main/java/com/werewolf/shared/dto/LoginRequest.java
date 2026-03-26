package com.werewolf.shared.dto;

public class LoginRequest extends Message {
    private String username;
    private String password;

    public LoginRequest() {
        this.type = "LOGIN_REQUEST";
    }

    public LoginRequest(String username, String password) {
        this();
        this.username = username;
        this.password = password;
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
}
