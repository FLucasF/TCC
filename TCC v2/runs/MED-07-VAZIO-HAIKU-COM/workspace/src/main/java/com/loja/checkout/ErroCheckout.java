package com.loja.checkout;

public class ErroCheckout extends Exception {
    private String codigo;

    public ErroCheckout(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
