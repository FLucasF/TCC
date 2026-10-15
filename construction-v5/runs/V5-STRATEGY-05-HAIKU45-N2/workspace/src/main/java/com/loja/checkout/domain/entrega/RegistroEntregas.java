package com.loja.checkout.domain.entrega;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.loja.checkout.domain.ModalidadeEntrega;

public class RegistroEntregas {
    private static final Map<ModalidadeEntrega, EstrategiaEntrega> entregas = new HashMap<>();

    static {
        entregas.put(ModalidadeEntrega.ECONOMICA, new EntregaEconomica());
        entregas.put(ModalidadeEntrega.EXPRESSA, new EntregaExpressa());
        entregas.put(ModalidadeEntrega.RETIRADA_LOJA, new EntregaRetiradaLoja());
        entregas.put(ModalidadeEntrega.MOTOBOY, new EntregaMotoboy());
    }

    public static Optional<EstrategiaEntrega> obter(ModalidadeEntrega modalidade) {
        return Optional.ofNullable(entregas.get(modalidade));
    }
}
