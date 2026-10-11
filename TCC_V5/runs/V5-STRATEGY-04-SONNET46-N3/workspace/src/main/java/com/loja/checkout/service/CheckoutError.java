package com.loja.checkout.service;

public class CheckoutError extends RuntimeException {
    private final String code;

    public CheckoutError(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
