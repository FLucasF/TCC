package com.pousadamarealta.model;

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
        return new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado");
    }

    public static ApiException reservationNotFound() {
        return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
    }

    public static ApiException roomUnavailable() {
        return new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado para o período");
    }

    public static ApiException operationNotAllowed() {
        return new ApiException(409, "OPERATION_NOT_ALLOWED", "Operação não permitida para o estado atual da reserva");
    }

    public static ApiException capacityExceeded() {
        return new ApiException(422, "CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto");
    }
}
