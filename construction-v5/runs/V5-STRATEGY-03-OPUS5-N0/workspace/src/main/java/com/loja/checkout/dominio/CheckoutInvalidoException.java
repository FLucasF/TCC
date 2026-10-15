package com.loja.checkout.dominio;

/** Lancada quando o pedido nao pode ser calculado. */
public class CheckoutInvalidoException extends RuntimeException {

    private final CodigoErro codigo;

    public CheckoutInvalidoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
