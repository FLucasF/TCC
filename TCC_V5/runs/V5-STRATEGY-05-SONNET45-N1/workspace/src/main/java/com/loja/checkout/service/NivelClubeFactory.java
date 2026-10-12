package com.loja.checkout.service;

import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.clube.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NivelClubeFactory {
    private final Map<String, NivelClube> niveis = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    public NivelClube obter(String codigo) {
        return niveis.get(codigo);
    }

    public boolean existe(String codigo) {
        return niveis.containsKey(codigo);
    }
}
