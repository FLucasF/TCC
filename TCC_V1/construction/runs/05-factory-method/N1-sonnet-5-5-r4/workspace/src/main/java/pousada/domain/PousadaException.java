package pousada.domain;

public class PousadaException extends RuntimeException {
    private final ErrorCode code;

    public PousadaException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }
}
