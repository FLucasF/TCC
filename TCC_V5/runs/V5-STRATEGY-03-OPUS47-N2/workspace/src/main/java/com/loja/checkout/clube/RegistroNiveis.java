package com.loja.checkout.clube;

import com.loja.checkout.ErroPedido;

import java.util.Map;

public class RegistroNiveis {
    private static final Map<String, NivelClube> NIVEIS = Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro()
    );

    public NivelClube obter(String nome) {
        if (nome == null) throw new ErroPedido("NIVEL_CLUBE_INVALIDO");
        NivelClube n = NIVEIS.get(nome);
        if (n == null) throw new ErroPedido("NIVEL_CLUBE_INVALIDO");
        return n;
    }
}
