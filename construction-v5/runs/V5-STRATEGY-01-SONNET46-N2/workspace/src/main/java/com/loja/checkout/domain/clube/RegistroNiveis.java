package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class RegistroNiveis {

    private final Map<String, NivelClube> niveis;

    public RegistroNiveis() {
        this.niveis = Map.of(
                "BRONZE", new Bronze(),
                "PRATA", new Prata(),
                "OURO", new Ouro()
        );
    }

    public Optional<NivelClube> buscar(String codigo) {
        return Optional.ofNullable(niveis.get(codigo));
    }
}
