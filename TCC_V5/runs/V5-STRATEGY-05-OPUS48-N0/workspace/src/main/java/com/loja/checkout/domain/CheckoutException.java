package com.loja.checkout.domain;

/**
 * Exceção de negócio: o pedido foi recusado e devolvemos apenas o código do problema.
 */
public class CheckoutException extends RuntimeException {

    private final transient CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
