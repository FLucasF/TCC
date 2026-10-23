package br.com.pousada.exception;

public class OperationNotAllowedException extends RuntimeException {
    public OperationNotAllowedException(String message) {
        super(message);
    }
}
