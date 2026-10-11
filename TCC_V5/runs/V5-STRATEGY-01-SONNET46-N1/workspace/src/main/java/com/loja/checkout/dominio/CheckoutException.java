package com.loja.checkout.dominio;

public class CheckoutException extends RuntimeException {
    public final CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }
}
