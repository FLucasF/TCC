package br.com.marealta.exception;

public class CapacityExceededException extends PousadaException {
    public CapacityExceededException(String message) {
        super("CAPACITY_EXCEEDED", message);
    }

    @Override
    public int getHttpStatus() {
        return 422;
    }
}
