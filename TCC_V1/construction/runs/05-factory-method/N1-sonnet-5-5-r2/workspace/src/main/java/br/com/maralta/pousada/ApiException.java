package br.com.maralta.pousada;

public class ApiException extends RuntimeException {
    private final int httpStatus;
    private final String code;

    public ApiException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public static ApiException invalidRequest(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }

    public static ApiException roomNotFound(int number) {
        return new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe");
    }

    public static ApiException reservationNotFound(String id) {
        return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
    }

    public static ApiException roomUnavailable(int number) {
        return new ApiException(409, "ROOM_UNAVAILABLE", "Quarto " + number + " já reservado em alguma das noites");
    }

    public static ApiException operationNotAllowed(String message) {
        return new ApiException(409, "OPERATION_NOT_ALLOWED", message);
    }

    public static ApiException capacityExceeded(int capacity) {
        return new ApiException(422, "CAPACITY_EXCEEDED", "O quarto comporta no máximo " + capacity + " hóspedes");
    }
}
