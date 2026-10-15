package com.loja.checkout.erro;

public class ErroNegocioException extends RuntimeException {

    private final String codigo;

    public ErroNegocioException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
