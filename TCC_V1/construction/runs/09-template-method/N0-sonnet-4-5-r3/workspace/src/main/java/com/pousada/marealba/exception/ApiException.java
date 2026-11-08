package com.pousada.marealba.exception;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int httpStatus;

    public ApiException(ErrorCode errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public static ApiException invalidRequest(String message) {
        return new ApiException(ErrorCode.INVALID_REQUEST, message, 400);
    }

    public static ApiException roomNotFound() {
        return new ApiException(ErrorCode.ROOM_NOT_FOUND, "Room not found", 404);
    }

    public static ApiException reservationNotFound() {
        return new ApiException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found", 404);
    }

    public static ApiException roomUnavailable() {
        return new ApiException(ErrorCode.ROOM_UNAVAILABLE, "Room is not available for the selected dates", 409);
    }

    public static ApiException operationNotAllowed(String message) {
        return new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, message, 409);
    }

    public static ApiException capacityExceeded() {
        return new ApiException(ErrorCode.CAPACITY_EXCEEDED, "Number of guests exceeds room capacity", 422);
    }
}
