package br.com.marealta;

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

    public static ApiException invalid(String m) { return new ApiException(400, "INVALID_REQUEST", m); }
    public static ApiException roomNotFound(Object n) { return new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado: " + n); }
    public static ApiException reservationNotFound(Object id) { return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + id); }
    public static ApiException unavailable(String m) { return new ApiException(409, "ROOM_UNAVAILABLE", m); }
    public static ApiException notAllowed(String m) { return new ApiException(409, "OPERATION_NOT_ALLOWED", m); }
    public static ApiException capacity(String m) { return new ApiException(422, "CAPACITY_EXCEEDED", m); }
}
