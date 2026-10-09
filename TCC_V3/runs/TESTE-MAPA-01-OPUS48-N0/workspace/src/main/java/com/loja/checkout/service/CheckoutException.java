package com.loja.checkout.service;

/**
 * Lançada quando o pedido não pode ser calculado. Carrega apenas o código
 * do problema, que é o que o serviço devolve para o site.
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
