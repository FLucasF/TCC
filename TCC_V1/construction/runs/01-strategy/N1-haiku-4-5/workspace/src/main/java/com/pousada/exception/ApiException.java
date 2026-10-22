package com.pousada.exception;

public class ApiException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public ApiException(String errorCode, int statusCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static class RoomNotFound extends ApiException {
        public RoomNotFound() {
            super("ROOM_NOT_FOUND", 404, "Room not found");
        }
    }

    public static class ReservationNotFound extends ApiException {
        public ReservationNotFound() {
            super("RESERVATION_NOT_FOUND", 404, "Reservation not found");
        }
    }

    public static class RoomUnavailable extends ApiException {
        public RoomUnavailable() {
            super("ROOM_UNAVAILABLE", 409, "Room is not available for the selected dates");
        }
    }

    public static class CapacityExceeded extends ApiException {
        public CapacityExceeded() {
            super("CAPACITY_EXCEEDED", 422, "Number of guests exceeds room capacity");
        }
    }

    public static class OperationNotAllowed extends ApiException {
        public OperationNotAllowed() {
            super("OPERATION_NOT_ALLOWED", 409, "This operation is not allowed for the current reservation status");
        }
    }

    public static class InvalidRequest extends ApiException {
        public InvalidRequest(String message) {
            super("INVALID_REQUEST", 400, message);
        }
    }
}
