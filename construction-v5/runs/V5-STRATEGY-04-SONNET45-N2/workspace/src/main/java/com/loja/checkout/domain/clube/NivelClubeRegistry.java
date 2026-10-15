package com.loja.checkout.domain.clube;

import java.util.HashMap;
import java.util.Map;

public class NivelClubeRegistry {
    private static final Map<String, NivelClube> niveis = new HashMap<>();

    static {
        niveis.put("BRONZE", new Bronze());
        niveis.put("PRATA", new Prata());
        niveis.put("OURO", new Ouro());
    }

    public static NivelClube obter(String codigo) {
        return niveis.get(codigo);
    }
}
