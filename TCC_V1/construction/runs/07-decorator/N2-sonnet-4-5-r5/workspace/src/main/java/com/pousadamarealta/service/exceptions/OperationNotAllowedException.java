package com.pousadamarealta.service.exceptions;

public class OperationNotAllowedException extends RuntimeException {
    public OperationNotAllowedException(String message) {
        super(message);
    }
}
