package com.loja.checkout.domain.modalidade;

import java.util.HashMap;
import java.util.Map;

public class RegistroModalidades {
    private static final Map<String, Modalidade> MODALIDADES = new HashMap<>();

    static {
        MODALIDADES.put("ECONOMICA", new ModalidadeEconomica());
        MODALIDADES.put("EXPRESSA", new ModalidadeExpressa());
        MODALIDADES.put("RETIRADA_LOJA", new ModalidadeRetiradaLoja());
        MODALIDADES.put("MOTOBOY", new ModalidadeMotoboy());
    }

    public static Modalidade obter(String codigo) {
        return MODALIDADES.get(codigo);
    }

    public static boolean existe(String codigo) {
        return MODALIDADES.containsKey(codigo);
    }
}
