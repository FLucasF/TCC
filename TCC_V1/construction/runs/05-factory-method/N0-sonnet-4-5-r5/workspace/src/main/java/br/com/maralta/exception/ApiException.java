package br.com.maralta.exception;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public ApiException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static ApiException invalidRequest(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }

    public static ApiException roomNotFound() {
        return new ApiException(404, "ROOM_NOT_FOUND", "Room not found");
    }

    public static ApiException reservationNotFound() {
        return new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
    }

    public static ApiException roomUnavailable() {
        return new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for the selected dates");
    }

    public static ApiException operationNotAllowed(String message) {
        return new ApiException(409, "OPERATION_NOT_ALLOWED", message);
    }

    public static ApiException capacityExceeded() {
        return new ApiException(422, "CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
    }
}
