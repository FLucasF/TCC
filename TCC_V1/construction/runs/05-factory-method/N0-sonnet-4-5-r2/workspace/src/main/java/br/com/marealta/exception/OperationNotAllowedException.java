package br.com.marealta.exception;

public class OperationNotAllowedException extends PousadaException {
    public OperationNotAllowedException(String message) {
        super("OPERATION_NOT_ALLOWED", message);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
