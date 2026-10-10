package com.loja.checkout.domain.entrega;

import java.util.HashMap;
import java.util.Map;

public class ModalidadeEntregaFactory {

    private static final Map<String, ModalidadeEntrega> MODALIDADES = new HashMap<>();

    static {
        MODALIDADES.put("ECONOMICA", new Economica());
        MODALIDADES.put("EXPRESSA", new Expressa());
        MODALIDADES.put("RETIRADA_LOJA", new RetiradaLoja());
        MODALIDADES.put("MOTOBOY", new Motoboy());
    }

    public static ModalidadeEntrega criar(String codigo) {
        return MODALIDADES.get(codigo);
    }

    public static boolean existe(String codigo) {
        return MODALIDADES.containsKey(codigo);
    }
}
