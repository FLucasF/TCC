package com.pousada.service;

public class OperationNotAllowedException extends RuntimeException {
    public OperationNotAllowedException(String message) {
        super(message);
    }
}
