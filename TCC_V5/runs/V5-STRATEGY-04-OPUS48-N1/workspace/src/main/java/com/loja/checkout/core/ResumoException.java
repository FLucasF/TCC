package com.loja.checkout.core;

/**
 * Pedido recusado. Carrega apenas o código do problema, na ordem definida pelo enunciado.
 */
public class ResumoException extends RuntimeException {

    private final String codigo;

    public ResumoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
