package br.com.marealta.pousada.domain;

public class PousadaException extends RuntimeException {
    private final ErrorCode code;

    public PousadaException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
