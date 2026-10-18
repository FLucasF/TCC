package com.shop.exception;

public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public BusinessException(String errorCode, int httpStatus) {
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
