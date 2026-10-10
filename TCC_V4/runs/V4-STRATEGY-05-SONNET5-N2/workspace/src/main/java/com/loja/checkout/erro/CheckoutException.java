package com.loja.checkout.erro;

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
