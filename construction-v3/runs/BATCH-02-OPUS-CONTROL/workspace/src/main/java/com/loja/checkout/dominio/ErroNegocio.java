package com.loja.checkout.dominio;

/** Erro de negocio devolvido ao site como {"erro": "CODIGO"}. */
public class ErroNegocio extends RuntimeException {

    private final CodigoErro codigo;

    public ErroNegocio(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
