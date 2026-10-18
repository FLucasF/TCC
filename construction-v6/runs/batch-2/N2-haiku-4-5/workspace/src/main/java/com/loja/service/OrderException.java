package com.loja.service;

public class OrderException extends Exception {
    public final String errorCode;
    public final int httpStatus;

    public OrderException(String errorCode, int httpStatus) {
        super(errorCode);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
