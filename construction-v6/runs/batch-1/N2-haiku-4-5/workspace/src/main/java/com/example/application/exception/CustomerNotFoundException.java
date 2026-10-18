package com.example.application.exception;

public class CustomerNotFoundException extends ApplicationException {
    public CustomerNotFoundException() {
        super("CUSTOMER_NOT_FOUND", 404);
    }
}
