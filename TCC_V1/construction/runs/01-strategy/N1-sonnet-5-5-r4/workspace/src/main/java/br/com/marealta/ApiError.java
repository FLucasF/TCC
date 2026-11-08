package br.com.marealta;

public class ApiError extends RuntimeException {
    private final int status;
    private final String code;

    private ApiError(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public static ApiError invalidRequest(String message) {
        return new ApiError(400, "INVALID_REQUEST", message);
    }

    public static ApiError roomNotFound(int number) {
        return new ApiError(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe");
    }

    public static ApiError reservationNotFound(String id) {
        return new ApiError(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
    }

    public static ApiError roomUnavailable() {
        return new ApiError(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
    }

    public static ApiError operationNotAllowed(String message) {
        return new ApiError(409, "OPERATION_NOT_ALLOWED", message);
    }

    public static ApiError capacityExceeded(int capacity) {
        return new ApiError(422, "CAPACITY_EXCEEDED", "O quarto comporta no máximo " + capacity + " hóspedes");
    }
}
