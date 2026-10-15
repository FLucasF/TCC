package com.loja;

public class CheckoutException extends Exception {
    private final String errorCode;

    public CheckoutException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
