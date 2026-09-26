package com.loja.checkout.domain.erro;

public class RegraNegocioException extends RuntimeException {

    private final CodigoErro codigo;

    public RegraNegocioException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
