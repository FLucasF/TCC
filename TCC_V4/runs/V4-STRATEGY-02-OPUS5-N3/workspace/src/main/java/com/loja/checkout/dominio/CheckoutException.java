package com.loja.checkout.dominio;

/** Pedido recusado: carrega o código do problema encontrado. */
public class CheckoutException extends RuntimeException {

    private final ErroCheckout erro;

    public CheckoutException(ErroCheckout erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroCheckout erro() {
        return erro;
    }
}
