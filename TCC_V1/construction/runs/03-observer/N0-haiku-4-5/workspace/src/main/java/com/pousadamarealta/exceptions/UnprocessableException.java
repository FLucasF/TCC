package com.pousadamarealta.exceptions;

public class UnprocessableException extends RuntimeException {
    public final String code;

    public UnprocessableException(String code, String message) {
        super(message);
        this.code = code;
    }
}
