package com.loja.resumo;

public class ErroResumo extends RuntimeException {

    private final String codigo;

    public ErroResumo(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
