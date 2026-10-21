package pousada;

public class PousadaException extends RuntimeException {
    public final ErrorCode code;

    public PousadaException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }
}
