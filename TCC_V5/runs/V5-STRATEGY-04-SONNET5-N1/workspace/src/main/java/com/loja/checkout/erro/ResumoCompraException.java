package com.loja.checkout.erro;

public class ResumoCompraException extends RuntimeException {

    private final CodigoErro codigo;

    public ResumoCompraException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
