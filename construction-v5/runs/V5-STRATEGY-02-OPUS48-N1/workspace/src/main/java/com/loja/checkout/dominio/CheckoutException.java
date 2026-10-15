package com.loja.checkout.dominio;

/**
 * Recusa de pedido. Carrega só o código do problema, que é tudo o que o
 * serviço devolve quando não dá para calcular.
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
