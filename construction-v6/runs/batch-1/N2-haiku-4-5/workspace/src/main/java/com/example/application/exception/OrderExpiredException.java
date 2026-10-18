package com.example.application.exception;

public class OrderExpiredException extends ApplicationException {
    public OrderExpiredException() {
        super("ORDER_EXPIRED", 409);
    }
}
