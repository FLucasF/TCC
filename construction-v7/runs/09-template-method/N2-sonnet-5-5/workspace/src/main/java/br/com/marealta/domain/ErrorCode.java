package br.com.marealta.domain;

public enum ErrorCode {
    INVALID_REQUEST(400),
    ROOM_NOT_FOUND(404),
    RESERVATION_NOT_FOUND(404),
    ROOM_UNAVAILABLE(409),
    OPERATION_NOT_ALLOWED(409),
    CAPACITY_EXCEEDED(422);

    private final int status;

    ErrorCode(int status) { this.status = status; }

    public int status() { return status; }
}
