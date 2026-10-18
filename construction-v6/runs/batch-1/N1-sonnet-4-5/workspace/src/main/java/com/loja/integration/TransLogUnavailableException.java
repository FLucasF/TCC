package com.loja.integration;

public class TransLogUnavailableException extends RuntimeException {
    public TransLogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
