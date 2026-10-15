package com.loja.checkout.factory;

import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.modalidade.*;
import java.util.Map;

public class ModalidadeFactory {

    private static final Map<String, ModalidadeEntrega> MODALIDADES = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
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
