package com.loja.service;

public class CheckoutException extends Exception {
    public final String errorCode;

    public CheckoutException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }
}
