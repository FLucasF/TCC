package pousada;

public class ApiException extends RuntimeException {
    public enum Code {
        INVALID_REQUEST(400),
        ROOM_NOT_FOUND(404),
        RESERVATION_NOT_FOUND(404),
        ROOM_UNAVAILABLE(409),
        OPERATION_NOT_ALLOWED(409),
        CAPACITY_EXCEEDED(422);

        public final int status;

        Code(int status) {
            this.status = status;
        }
    }

    private final Code code;

    public ApiException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public static ApiException invalid(String message) {
        return new ApiException(Code.INVALID_REQUEST, message);
    }

    public static ApiException notAllowed(String message) {
        return new ApiException(Code.OPERATION_NOT_ALLOWED, message);
    }
}
