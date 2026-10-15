package com.loja.resumo.calculo;

public class ResumoException extends RuntimeException {

    private final String codigo;

    public ResumoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
