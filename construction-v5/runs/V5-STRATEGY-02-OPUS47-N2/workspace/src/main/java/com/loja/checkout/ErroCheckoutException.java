package com.loja.checkout;

public class ErroCheckoutException extends RuntimeException {
    private final String codigo;

    public ErroCheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
