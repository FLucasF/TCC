package com.loja.checkout.factory;

import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.nivel.*;
import java.util.Map;

public class NivelClubeFactory {

    private static final Map<String, NivelClube> NIVEIS = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    public static NivelClube obter(String codigo) {
        return NIVEIS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return NIVEIS.containsKey(codigo);
    }
}
