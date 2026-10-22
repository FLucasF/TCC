package br.com.marealta.domain;

public class AppException extends RuntimeException {
    private final int status;
    private final String code;

    public AppException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }

    public static AppException invalid(String message) { return new AppException(400, "INVALID_REQUEST", message); }
    public static AppException roomNotFound(int n) { return new AppException(404, "ROOM_NOT_FOUND", "Quarto " + n + " não existe"); }
    public static AppException reservationNotFound(String id) { return new AppException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe"); }
    public static AppException unavailable() { return new AppException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites"); }
    public static AppException notAllowed(String message) { return new AppException(409, "OPERATION_NOT_ALLOWED", message); }
    public static AppException capacity(int capacity) { return new AppException(422, "CAPACITY_EXCEEDED", "O quarto comporta no máximo " + capacity + " hóspedes"); }
}
