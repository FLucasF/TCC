package com.loja.checkout.domain.modalidade;

import java.util.HashMap;
import java.util.Map;

public class ModalidadeEntregaFactory {

    private static final Map<String, ModalidadeEntrega> modalidades = new HashMap<>();

    static {
        modalidades.put("ECONOMICA", new Economica());
        modalidades.put("EXPRESSA", new Expressa());
        modalidades.put("RETIRADA_LOJA", new RetiradaLoja());
        modalidades.put("MOTOBOY", new Motoboy());
    }

    public static ModalidadeEntrega obter(String codigo) {
        return modalidades.get(codigo);
    }
}
