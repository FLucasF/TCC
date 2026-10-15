package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.ErroPedido;
import java.util.Map;

public final class Niveis {
    private static final Map<String, NivelClube> TODOS = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    private Niveis() {}

    public static NivelClube resolver(String codigo) {
        NivelClube n = codigo == null ? null : TODOS.get(codigo);
        if (n == null) throw new ErroPedido("NIVEL_CLUBE_INVALIDO");
        return n;
    }
}
