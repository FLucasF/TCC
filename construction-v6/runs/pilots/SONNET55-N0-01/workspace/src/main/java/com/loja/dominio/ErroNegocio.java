package com.loja.dominio;

public class ErroNegocio extends RuntimeException {

    private final String codigo;
    private final int status;

    public ErroNegocio(String codigo, int status) {
        super(codigo);
        this.codigo = codigo;
        this.status = status;
    }

    public static ErroNegocio invalido(String codigo) {
        return new ErroNegocio(codigo, 422);
    }

    public static ErroNegocio conflito(String codigo) {
        return new ErroNegocio(codigo, 409);
    }

    public static ErroNegocio naoEncontrado(String codigo) {
        return new ErroNegocio(codigo, 404);
    }

    public String codigo() {
        return codigo;
    }

    public int status() {
        return status;
    }
}
