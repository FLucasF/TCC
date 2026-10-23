package br.com.pousada.marealta.exception;

public class AppException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public AppException(int statusCode, String errorCode, String message) {
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

    public static AppException invalidRequest(String message) {
        return new AppException(400, "INVALID_REQUEST", message);
    }

    public static AppException roomNotFound() {
        return new AppException(404, "ROOM_NOT_FOUND", "Quarto não encontrado");
    }

    public static AppException reservationNotFound() {
        return new AppException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
    }

    public static AppException roomUnavailable() {
        return new AppException(409, "ROOM_UNAVAILABLE", "Quarto já reservado neste período");
    }

    public static AppException operationNotAllowed() {
        return new AppException(409, "OPERATION_NOT_ALLOWED", "Operação não permitida para o status atual da reserva");
    }

    public static AppException capacityExceeded() {
        return new AppException(422, "CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto");
    }
}
