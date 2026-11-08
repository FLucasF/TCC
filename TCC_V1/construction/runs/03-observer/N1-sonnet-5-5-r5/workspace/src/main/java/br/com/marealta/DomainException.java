package br.com.marealta;

/** Erro de negócio que vira { "error": code, "message": ... } com o status HTTP indicado. */
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

    public static DomainException invalidRequest(String message) {
        return new DomainException(400, "INVALID_REQUEST", message);
    }

    public static DomainException roomNotFound(int number) {
        return new DomainException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe");
    }

    public static DomainException reservationNotFound(String id) {
        return new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
    }

    public static DomainException roomUnavailable(int number) {
        return new DomainException(409, "ROOM_UNAVAILABLE", "Quarto " + number + " já reservado em alguma das noites");
    }

    public static DomainException notAllowed(String message) {
        return new DomainException(409, "OPERATION_NOT_ALLOWED", message);
    }

    public static DomainException capacityExceeded(int number, int capacity) {
        return new DomainException(422, "CAPACITY_EXCEEDED",
                "Quarto " + number + " comporta no máximo " + capacity + " pessoas");
    }
}
