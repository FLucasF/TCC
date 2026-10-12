package com.loja.entrega;

public class ModalidadeEntregaFactory {

    public static ModalidadeEntrega criar(String codigo) {
        return switch (codigo) {
            case "ECONOMICA" -> new Economica();
            case "EXPRESSA" -> new Expressa();
            case "RETIRADA_LOJA" -> new RetiradaLoja();
            case "MOTOBOY" -> new Motoboy();
            default -> null;
        };
    }
}
