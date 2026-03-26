package com.werewolf.shared.dto;

public class LoginResponse extends Message {
    private boolean success;
    private UserDTO user;
    private String errorMessage;

    public LoginResponse() {
        this.type = "LOGIN_RESPONSE";
    }

    public LoginResponse(boolean success, UserDTO user, String errorMessage) {
        this();
        this.success = success;
        this.user = user;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
