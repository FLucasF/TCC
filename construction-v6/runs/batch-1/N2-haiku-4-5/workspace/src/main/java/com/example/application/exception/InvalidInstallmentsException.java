package com.example.application.exception;

public class InvalidInstallmentsException extends ApplicationException {
    public InvalidInstallmentsException() {
        super("INVALID_INSTALLMENTS", 422);
    }
}
