package com.loja.checkout.service;

/**
 * Lancada quando o pedido nao pode ser calculado. Carrega o codigo do problema,
 * que e o que o site recebe na resposta de erro.
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
