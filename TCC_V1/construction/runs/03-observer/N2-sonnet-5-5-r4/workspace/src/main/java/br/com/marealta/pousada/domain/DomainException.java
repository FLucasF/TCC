package br.com.marealta.pousada.domain;

public class DomainException extends RuntimeException {

    public enum Kind {
        INVALID_REQUEST(400),
        ROOM_NOT_FOUND(404),
        RESERVATION_NOT_FOUND(404),
        ROOM_UNAVAILABLE(409),
        OPERATION_NOT_ALLOWED(409),
        CAPACITY_EXCEEDED(422);

        private final int httpStatus;

        Kind(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int httpStatus() {
            return httpStatus;
        }
    }

    private final Kind kind;

    public DomainException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
