package com.loja.checkout.dominio;

/** Erro de negocio do checkout; vira uma resposta 400 com o codigo correspondente. */
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
