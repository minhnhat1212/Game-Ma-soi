package com.werewolf.shared.dto;

import com.werewolf.shared.enums.ErrorCode;

public class ErrorResponse extends Message {
    private int errorCode;
    private String errorMessage;

    public ErrorResponse() {
        this.type = "ERROR_RESPONSE";
    }

    public ErrorResponse(ErrorCode errorCode) {
        this();
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    public ErrorResponse(int errorCode, String errorMessage) {
        this();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
