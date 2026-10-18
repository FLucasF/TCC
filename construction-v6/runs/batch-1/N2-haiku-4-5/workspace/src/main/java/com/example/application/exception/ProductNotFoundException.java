package com.example.application.exception;

public class ProductNotFoundException extends ApplicationException {
    public ProductNotFoundException() {
        super("PRODUCT_NOT_FOUND", 404);
    }
}
