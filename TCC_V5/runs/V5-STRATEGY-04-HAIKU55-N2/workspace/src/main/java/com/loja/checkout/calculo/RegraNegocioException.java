package com.loja.checkout.calculo;

public class RegraNegocioException extends RuntimeException {

    private final CodigoErro codigo;

    public RegraNegocioException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
