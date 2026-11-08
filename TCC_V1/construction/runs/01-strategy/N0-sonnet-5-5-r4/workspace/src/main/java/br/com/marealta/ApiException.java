package br.com.marealta;

/** Erro de negócio/validação que vira { "error": CODE, "message": ... } com o status HTTP indicado. */
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }

    public static ApiException invalid(String message) { return new ApiException(400, "INVALID_REQUEST", message); }
    public static ApiException roomNotFound(String message) { return new ApiException(404, "ROOM_NOT_FOUND", message); }
    public static ApiException reservationNotFound(String message) { return new ApiException(404, "RESERVATION_NOT_FOUND", message); }
    public static ApiException roomUnavailable(String message) { return new ApiException(409, "ROOM_UNAVAILABLE", message); }
    public static ApiException notAllowed(String message) { return new ApiException(409, "OPERATION_NOT_ALLOWED", message); }
    public static ApiException capacityExceeded(String message) { return new ApiException(422, "CAPACITY_EXCEEDED", message); }
}
