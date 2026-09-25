package com.loja.checkout.exception;

public class NegocioException extends RuntimeException {

    private final String codigo;

    public NegocioException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
