package com.loja.checkout.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioEntregas {
    private static final Map<String, ModalidadeEntrega> entregas = new HashMap<>();

    static {
        entregas.put("ECONOMICA", new EntregaEconomica());
        entregas.put("EXPRESSA", new EntregaExpressa());
        entregas.put("RETIRADA_LOJA", new EntregaRetiradaLoja());
        entregas.put("MOTOBOY", new EntregaMotoboy());
    }

    public static Optional<ModalidadeEntrega> obter(String codigo) {
        return Optional.ofNullable(entregas.get(codigo));
    }

    public static boolean existe(String codigo) {
        return entregas.containsKey(codigo);
    }
}
