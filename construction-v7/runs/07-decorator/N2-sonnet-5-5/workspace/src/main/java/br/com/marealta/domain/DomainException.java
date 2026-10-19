package br.com.marealta.domain;

public class DomainException extends RuntimeException {
    private final int status;
    private final String code;

    public DomainException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }

    public static DomainException invalidRequest(String message) {
        return new DomainException(400, "INVALID_REQUEST", message);
    }

    public static DomainException roomNotFound(String number) {
        return new DomainException(404, "ROOM_NOT_FOUND", "Quarto não encontrado: " + number);
    }

    public static DomainException reservationNotFound(String id) {
        return new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + id);
    }

    public static DomainException roomUnavailable() {
        return new DomainException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites pedidas");
    }

    public static DomainException notAllowed(String message) {
        return new DomainException(409, "OPERATION_NOT_ALLOWED", message);
    }

    public static DomainException capacityExceeded(int capacity) {
        return new DomainException(422, "CAPACITY_EXCEEDED", "O quarto comporta no máximo " + capacity + " hóspedes");
    }
}
