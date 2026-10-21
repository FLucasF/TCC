package br.com.marealta.reservas.domain;

public class DomainException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    private DomainException(String code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static DomainException invalidRequest(String message) {
        return new DomainException("INVALID_REQUEST", 400, message);
    }

    public static DomainException roomNotFound(int number) {
        return new DomainException("ROOM_NOT_FOUND", 404, "Quarto " + number + " não existe");
    }

    public static DomainException reservationNotFound(String id) {
        return new DomainException("RESERVATION_NOT_FOUND", 404, "Reserva " + id + " não existe");
    }

    public static DomainException roomUnavailable(int number) {
        return new DomainException("ROOM_UNAVAILABLE", 409, "Quarto " + number + " já reservado em alguma das noites");
    }

    public static DomainException operationNotAllowed(String message) {
        return new DomainException("OPERATION_NOT_ALLOWED", 409, message);
    }

    public static DomainException capacityExceeded(int capacity) {
        return new DomainException("CAPACITY_EXCEEDED", 422, "O quarto comporta no máximo " + capacity + " hóspedes");
    }

    public String code() { return code; }

    public int httpStatus() { return httpStatus; }
}
