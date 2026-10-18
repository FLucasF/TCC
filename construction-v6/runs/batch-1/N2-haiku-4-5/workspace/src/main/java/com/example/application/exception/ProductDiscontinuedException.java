package com.example.application.exception;

public class ProductDiscontinuedException extends ApplicationException {
    public ProductDiscontinuedException() {
        super("PRODUCT_DISCONTINUED", 422);
    }
}
