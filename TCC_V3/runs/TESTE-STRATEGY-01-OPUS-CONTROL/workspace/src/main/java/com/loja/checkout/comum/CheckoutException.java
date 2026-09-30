package com.loja.checkout.comum;

/** Erro de negocio devolvido ao site como {"erro": "CODIGO"} com status 400. */
public class CheckoutException extends RuntimeException {

    private final CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
