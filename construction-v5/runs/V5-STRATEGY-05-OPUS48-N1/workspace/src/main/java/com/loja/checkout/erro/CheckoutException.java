package com.loja.checkout.erro;

/**
 * Pedido recusado: carrega o codigo do primeiro problema encontrado.
 */
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
