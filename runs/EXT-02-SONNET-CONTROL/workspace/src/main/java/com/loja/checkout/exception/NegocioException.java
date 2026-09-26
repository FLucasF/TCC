package com.loja.checkout.exception;

public class NegocioException extends RuntimeException {

    private final CodigoErro codigo;

    public NegocioException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
