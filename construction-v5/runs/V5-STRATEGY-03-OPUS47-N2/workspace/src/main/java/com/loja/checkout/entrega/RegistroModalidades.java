package com.loja.checkout.entrega;

import com.loja.checkout.ErroPedido;

import java.util.Map;

public class RegistroModalidades {
    private static final Map<String, ModalidadeEntrega> MODALIDADES = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy()
    );

    public ModalidadeEntrega obter(String nome) {
        if (nome == null) throw new ErroPedido("MODALIDADE_INVALIDA");
        ModalidadeEntrega m = MODALIDADES.get(nome);
        if (m == null) throw new ErroPedido("MODALIDADE_INVALIDA");
        return m;
    }
}
