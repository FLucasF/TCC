package com.loja.domain.clube;

import java.util.Map;

public class NivelClubeFactory {
    private static final Map<String, NivelClube> NIVEIS = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    public static NivelClube criar(String nome) {
        return NIVEIS.get(nome);
    }
}
