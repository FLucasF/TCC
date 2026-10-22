package pousada.domain;

public class DomainException extends RuntimeException {
    public enum Code {
        INVALID_REQUEST, ROOM_NOT_FOUND, RESERVATION_NOT_FOUND,
        ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED, CAPACITY_EXCEEDED
    }

    private final Code code;

    public DomainException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
