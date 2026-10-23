package br.com.marealta.exception;

public abstract class PousadaException extends RuntimeException {
    private final String errorCode;

    public PousadaException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public abstract int getHttpStatus();
}
