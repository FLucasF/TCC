package com.loja.checkout.comum;

/** Pedido recusado; carrega o código do problema devolvido ao site. */
public class ResumoException extends RuntimeException {

    private final String codigo;

    public ResumoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
