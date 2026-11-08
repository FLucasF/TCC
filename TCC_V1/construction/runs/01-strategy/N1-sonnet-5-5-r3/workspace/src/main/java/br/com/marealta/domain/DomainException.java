package br.com.marealta.domain;

public class DomainException extends RuntimeException {
    public enum Code {
        INVALID_REQUEST(400),
        ROOM_NOT_FOUND(404),
        RESERVATION_NOT_FOUND(404),
        ROOM_UNAVAILABLE(409),
        OPERATION_NOT_ALLOWED(409),
        CAPACITY_EXCEEDED(422);

        private final int httpStatus;

        Code(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int httpStatus() {
            return httpStatus;
        }
    }

    private final Code code;

    public DomainException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public static DomainException invalid(String message) {
        return new DomainException(Code.INVALID_REQUEST, message);
    }
}
