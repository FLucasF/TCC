package com.loja.checkout.dominio;

public class ErroCheckoutException extends RuntimeException {

    private final ErroCheckout erro;

    public ErroCheckoutException(ErroCheckout erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroCheckout erro() {
        return erro;
    }
}
