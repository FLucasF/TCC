package com.loja.pedidos.exception;

import org.springframework.http.HttpStatus;

public class PedidoException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus statusCode;

    public PedidoException(String errorCode, String message, HttpStatus statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
