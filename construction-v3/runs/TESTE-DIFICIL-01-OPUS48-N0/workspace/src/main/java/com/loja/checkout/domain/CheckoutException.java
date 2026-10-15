package com.loja.checkout.domain;

/**
 * Sinaliza que o pedido nao pode ser calculado. Carrega o codigo do problema
 * que o site espera receber (ex.: PEDIDO_INVALIDO, CUPOM_NAO_APLICAVEL).
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
