package br.com.pousadamarealta.reservas.domain;

public class PousadaException extends RuntimeException {
    public enum Code {
        INVALID_REQUEST(400), ROOM_NOT_FOUND(404), RESERVATION_NOT_FOUND(404),
        ROOM_UNAVAILABLE(409), OPERATION_NOT_ALLOWED(409), CAPACITY_EXCEEDED(422);

        public final int httpStatus;

        Code(int httpStatus) { this.httpStatus = httpStatus; }
    }

    private final Code code;

    public PousadaException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
