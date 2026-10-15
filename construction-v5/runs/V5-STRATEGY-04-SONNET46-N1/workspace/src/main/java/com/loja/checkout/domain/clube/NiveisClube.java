package com.loja.checkout.domain.clube;

import java.util.Map;

public class NiveisClube {
    private static final Map<String, BeneficiosClube> REGISTRO = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    public static BeneficiosClube buscar(String nivel) {
        return REGISTRO.get(nivel);
    }
}
