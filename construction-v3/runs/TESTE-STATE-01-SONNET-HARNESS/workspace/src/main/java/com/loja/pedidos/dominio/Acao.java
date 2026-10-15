package com.loja.pedidos.dominio;

public enum Acao {
    PAGAR,
    SEPARAR,
    ENVIAR,
    ENTREGAR,
    CANCELAR,
    DEVOLVER;

    public static Acao parse(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Acao.valueOf(texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
