package com.loja.checkout.excecao;

public class ErroCheckout extends RuntimeException {
    private final String codigo;

    public ErroCheckout(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
