package com.loja.checkout.dominio;

/** Excecao de negocio: carrega o codigo de erro que o site espera receber. */
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
