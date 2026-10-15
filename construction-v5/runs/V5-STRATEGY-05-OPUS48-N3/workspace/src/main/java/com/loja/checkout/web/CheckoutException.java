package com.loja.checkout.web;

/** Recusa do pedido, carregando só o código do problema a devolver. */
public class CheckoutException extends RuntimeException {

    private final String codigo;

    public CheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
