package com.loja.checkout.exception;

public class CheckoutException extends RuntimeException {

    private final String code;

    public CheckoutException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
