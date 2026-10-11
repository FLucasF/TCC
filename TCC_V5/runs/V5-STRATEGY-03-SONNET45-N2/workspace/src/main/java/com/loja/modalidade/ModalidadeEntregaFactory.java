package com.loja.modalidade;

import com.loja.exception.PedidoException;
import java.util.Map;

public class ModalidadeEntregaFactory {
    private static final Map<String, ModalidadeEntrega> MODALIDADES = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    public static ModalidadeEntrega criar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new PedidoException("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega modalidade = MODALIDADES.get(codigo);
        if (modalidade == null) {
            throw new PedidoException("MODALIDADE_INVALIDA");
        }

        return modalidade;
    }
}
