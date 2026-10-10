package com.loja.checkout.dominio;

/** Recusa do pedido: carrega apenas o codigo do problema encontrado. */
public class CheckoutException extends RuntimeException {

    private final ErroCheckout erro;

    public CheckoutException(ErroCheckout erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroCheckout getErro() {
        return erro;
    }
}
