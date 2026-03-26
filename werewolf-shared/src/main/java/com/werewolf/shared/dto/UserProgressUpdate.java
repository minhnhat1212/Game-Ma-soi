package com.werewolf.shared.dto;

/**
 * Push từ server khi user được cập nhật progression sau khi kết thúc game.
 */
public class UserProgressUpdate extends Message {
    private UserDTO user;

    public UserProgressUpdate() {
        this.type = "USER_PROGRESS_UPDATE";
    }

    public UserProgressUpdate(UserDTO user) {
        this();
        this.user = user;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}

