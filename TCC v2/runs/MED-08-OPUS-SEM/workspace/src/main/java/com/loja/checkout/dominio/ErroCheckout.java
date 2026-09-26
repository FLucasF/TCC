package com.loja.checkout.dominio;

/** Erro de negocio que vira uma resposta 400 com o codigo correspondente. */
public class ErroCheckout extends RuntimeException {

    private final CodigoErro codigo;

    public ErroCheckout(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
