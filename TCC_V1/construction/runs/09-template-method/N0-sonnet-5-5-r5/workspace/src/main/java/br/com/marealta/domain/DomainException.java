package br.com.marealta.domain;

public class DomainException extends RuntimeException {
    private final String code;
    private final int status;

    public DomainException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String code() { return code; }

    public int status() { return status; }

    public static DomainException invalid(String message) {
        return new DomainException(400, "INVALID_REQUEST", message);
    }

    public static DomainException roomNotFound(Object number) {
        return new DomainException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe");
    }

    public static DomainException reservationNotFound(Object id) {
        return new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
    }

    public static DomainException unavailable(String message) {
        return new DomainException(409, "ROOM_UNAVAILABLE", message);
    }

    public static DomainException notAllowed(String message) {
        return new DomainException(409, "OPERATION_NOT_ALLOWED", message);
    }

    public static DomainException capacity(String message) {
        return new DomainException(422, "CAPACITY_EXCEEDED", message);
    }
}
