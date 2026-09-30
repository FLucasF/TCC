package com.loja.checkout.dominio;

public class CheckoutInvalidoException extends RuntimeException {

    private final ErroCheckout erro;

    public CheckoutInvalidoException(ErroCheckout erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroCheckout erro() {
        return erro;
    }
}
