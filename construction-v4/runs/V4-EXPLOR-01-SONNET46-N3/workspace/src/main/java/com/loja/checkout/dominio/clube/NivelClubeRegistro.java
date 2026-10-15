package com.loja.checkout.dominio.clube;

import com.loja.checkout.infra.CheckoutException;

import java.util.Map;

public class NivelClubeRegistro {

    private static final Map<String, NivelClube> REGISTRO = Map.of(
            "BRONZE", new NivelBronze(),
            "PRATA", new NivelPrata(),
            "OURO", new NivelOuro()
    );

    public static NivelClube buscar(String codigo) {
        if (codigo == null || !REGISTRO.containsKey(codigo)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return REGISTRO.get(codigo);
    }
}
