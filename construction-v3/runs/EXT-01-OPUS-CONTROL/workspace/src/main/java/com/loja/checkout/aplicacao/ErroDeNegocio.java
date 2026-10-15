package com.loja.checkout.aplicacao;

/** Pedido recusado por uma regra de negocio. */
public class ErroDeNegocio extends RuntimeException {

    private final CodigoErro codigo;

    public ErroDeNegocio(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
