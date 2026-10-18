package com.example.application.exception;

public class OrderNotFoundException extends ApplicationException {
    public OrderNotFoundException() {
        super("ORDER_NOT_FOUND", 404);
    }
}
