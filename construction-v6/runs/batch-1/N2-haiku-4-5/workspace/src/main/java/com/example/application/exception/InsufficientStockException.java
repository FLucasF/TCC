package com.example.application.exception;

public class InsufficientStockException extends ApplicationException {
    public InsufficientStockException() {
        super("INSUFFICIENT_STOCK", 422);
    }
}
