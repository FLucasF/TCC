package com.loja.checkout.erro;

public class RegraNegocioException extends RuntimeException {

    private final String codigo;

    public RegraNegocioException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
