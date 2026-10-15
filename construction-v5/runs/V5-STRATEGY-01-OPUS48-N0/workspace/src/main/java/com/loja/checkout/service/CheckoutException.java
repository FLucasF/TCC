package com.loja.checkout.service;

/**
 * Lancada quando o pedido nao pode ser calculado. Carrega o codigo do
 * problema que sera devolvido ao site.
 */
public class CheckoutException extends RuntimeException {

    private final CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
