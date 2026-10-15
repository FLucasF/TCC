package com.loja.checkout.dominio;

/** Exceção de negócio: carrega o código do problema que deve ser devolvido ao site. */
public class CheckoutException extends RuntimeException {

    private final transient ErroCheckout erro;

    public CheckoutException(ErroCheckout erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroCheckout getErro() {
        return erro;
    }
}
