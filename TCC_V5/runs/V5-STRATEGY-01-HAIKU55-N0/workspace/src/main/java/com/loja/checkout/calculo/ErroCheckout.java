package com.loja.checkout.calculo;

public class ErroCheckout extends RuntimeException {

    private final String codigo;

    public ErroCheckout(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
