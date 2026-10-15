package com.loja.checkout.domain.entrega;

import java.util.Map;

public class ModalidadeEntregaFactory {

    private static final Map<String, ModalidadeEntrega> MODALIDADES = Map.of(
        "ECONOMICA", new EntregaEconomica(),
        "EXPRESSA", new EntregaExpressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    public static ModalidadeEntrega obter(String codigo) {
        return MODALIDADES.get(codigo);
    }

    public static boolean existe(String codigo) {
        return MODALIDADES.containsKey(codigo);
    }
}
