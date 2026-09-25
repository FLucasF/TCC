package com.loja.checkout.dominio;

/** Erro de negocio do checkout: vira uma resposta 400 com o codigo do erro. */
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
