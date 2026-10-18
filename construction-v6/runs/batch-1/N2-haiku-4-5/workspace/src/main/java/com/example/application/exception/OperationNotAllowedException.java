package com.example.application.exception;

public class OperationNotAllowedException extends ApplicationException {
    public OperationNotAllowedException() {
        super("OPERATION_NOT_ALLOWED", 409);
    }
}
