package com.pousadamarealta.exception;

public class AppException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public AppException(String errorCode, String message, int statusCode) {
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

    public static AppException invalidRequest(String message) {
        return new AppException("INVALID_REQUEST", message, 400);
    }

    public static AppException roomNotFound() {
        return new AppException("ROOM_NOT_FOUND", "Quarto não encontrado", 404);
    }

    public static AppException reservationNotFound() {
        return new AppException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
    }

    public static AppException roomUnavailable() {
        return new AppException("ROOM_UNAVAILABLE", "Quarto já reservado para as datas selecionadas", 409);
    }

    public static AppException operationNotAllowed(String message) {
        return new AppException("OPERATION_NOT_ALLOWED", message, 409);
    }

    public static AppException capacityExceeded() {
        return new AppException("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto", 422);
    }
}
