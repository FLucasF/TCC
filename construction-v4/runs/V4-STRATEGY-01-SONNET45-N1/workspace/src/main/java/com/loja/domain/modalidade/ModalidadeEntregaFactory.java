package com.loja.domain.modalidade;

import java.util.Map;

public class ModalidadeEntregaFactory {
    private static final Map<String, ModalidadeEntrega> MODALIDADES = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    public static ModalidadeEntrega criar(String nome) {
        return MODALIDADES.get(nome);
    }
}
