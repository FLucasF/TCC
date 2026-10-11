package com.loja.checkout.web;

public class CheckoutException extends RuntimeException {
    public CheckoutException(String codigo) {
        super(codigo);
    }
}
