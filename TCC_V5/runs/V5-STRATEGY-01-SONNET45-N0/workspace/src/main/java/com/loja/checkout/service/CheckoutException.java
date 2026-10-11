package com.loja.checkout.service;

public class CheckoutException extends RuntimeException {
    private final String errorCode;

    public CheckoutException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
