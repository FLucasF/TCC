package com.loja.checkout.error;

public class CheckoutException extends RuntimeException {
    private final String codigo;

    public CheckoutException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
