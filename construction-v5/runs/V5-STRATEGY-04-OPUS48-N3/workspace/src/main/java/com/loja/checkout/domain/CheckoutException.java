package com.loja.checkout.domain;

/**
 * Pedido recusado. Carrega só o código do problema, como o serviço devolve.
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
