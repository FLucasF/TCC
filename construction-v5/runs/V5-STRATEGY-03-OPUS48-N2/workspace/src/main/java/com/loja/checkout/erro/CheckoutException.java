package com.loja.checkout.erro;

/** Recusa de negocio: carrega o codigo do problema a ser devolvido ao site. */
public class CheckoutException extends RuntimeException {

    private final CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
