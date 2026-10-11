package com.loja.checkout.domain;

/**
 * Lancada quando o pedido nao pode ser calculado. Carrega o codigo de erro
 * (ex.: PEDIDO_INVALIDO) que o servico devolve para o site.
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
