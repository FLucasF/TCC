package com.pousadamarealta.exception;

public class OperationNotAllowedException extends ApiException {
    public OperationNotAllowedException(String message) {
        super("OPERATION_NOT_ALLOWED", message, 409);
    }
}
