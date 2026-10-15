package com.loja.checkout.service;

/**
 * Pedido recusado. Carrega apenas o código do problema, que é o que o site
 * recebe.
 */
public class CheckoutException extends RuntimeException {

    private final String codigo;

    public CheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
