package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NivelClubeRegistry {

    private final Map<String, NivelClube> niveis = Map.of(
            "BRONZE", new NivelBronze(),
            "PRATA", new NivelPrata(),
            "OURO", new NivelOuro()
    );

    public NivelClube buscar(String codigo) {
        return niveis.get(codigo);
    }
}
