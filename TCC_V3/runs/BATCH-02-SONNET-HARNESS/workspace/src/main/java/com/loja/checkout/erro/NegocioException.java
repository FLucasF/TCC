package com.loja.checkout.erro;

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
