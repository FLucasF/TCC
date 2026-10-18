package com.example.application.exception;

public abstract class ApplicationException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public ApplicationException(String errorCode, int httpStatus) {
        super(errorCode);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
