package com.loja.checkout.domain;

/**
 * Recusa de pedido. Carrega só o código do problema, que é o que o serviço
 * devolve: { "erro": CODIGO }.
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
