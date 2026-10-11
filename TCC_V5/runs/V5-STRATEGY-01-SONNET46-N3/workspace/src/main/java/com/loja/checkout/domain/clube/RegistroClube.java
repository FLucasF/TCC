package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class RegistroClube {

    private final Map<String, NivelClube> niveis = Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro()
    );

    public Optional<NivelClube> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(niveis.get(codigo));
    }
}
