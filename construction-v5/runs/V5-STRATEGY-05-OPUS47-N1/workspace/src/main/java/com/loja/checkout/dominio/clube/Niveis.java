package com.loja.checkout.dominio.clube;

import java.util.Map;
import java.util.Optional;

public final class Niveis {
    private static final Map<String, NivelClube> MAP = Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro()
    );

    private Niveis() {}

    public static Optional<NivelClube> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(MAP::get);
    }
}
