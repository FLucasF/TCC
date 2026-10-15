package com.loja.checkout;

import java.util.Arrays;

public class ResumoException extends RuntimeException {

    private final String codigo;

    public ResumoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    static <E extends Enum<E>> E paraEnum(E[] valores, String nome, String codigoErro) {
        return Arrays.stream(valores)
                .filter(valor -> valor.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new ResumoException(codigoErro));
    }
}
