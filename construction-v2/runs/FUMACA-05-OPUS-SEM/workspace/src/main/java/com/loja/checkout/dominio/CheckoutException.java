package com.loja.checkout.dominio;

/** Interrompe o calculo e devolve o primeiro erro encontrado. */
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
