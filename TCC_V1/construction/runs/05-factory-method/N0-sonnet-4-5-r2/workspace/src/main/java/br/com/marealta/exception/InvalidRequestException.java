package br.com.marealta.exception;

public class InvalidRequestException extends PousadaException {
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }
}
