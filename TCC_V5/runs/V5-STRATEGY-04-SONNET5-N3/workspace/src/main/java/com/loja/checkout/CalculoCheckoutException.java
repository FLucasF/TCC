package com.loja.checkout;

public class CalculoCheckoutException extends RuntimeException {

    private final String codigo;

    public CalculoCheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
