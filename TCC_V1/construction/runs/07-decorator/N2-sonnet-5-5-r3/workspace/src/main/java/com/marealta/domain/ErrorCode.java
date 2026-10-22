package com.marealta.domain;

public enum ErrorCode {
    INVALID_REQUEST(400),
    ROOM_NOT_FOUND(404),
    RESERVATION_NOT_FOUND(404),
    ROOM_UNAVAILABLE(409),
    OPERATION_NOT_ALLOWED(409),
    CAPACITY_EXCEEDED(422);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
