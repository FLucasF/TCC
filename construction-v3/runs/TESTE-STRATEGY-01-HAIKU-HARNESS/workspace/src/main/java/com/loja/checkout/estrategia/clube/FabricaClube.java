package com.loja.checkout.estrategia.clube;

import com.loja.checkout.modelo.NivelClube;

public class FabricaClube {
    public static EstrategiaClube criar(String nivelStr) {
        if (nivelStr == null || nivelStr.isBlank()) {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            NivelClube nivel = NivelClube.valueOf(nivelStr);
            return switch (nivel) {
                case BRONZE -> new ClubeBronze();
                case PRATA -> new ClubePrata();
                case OURO -> new ClubeOuro();
            };
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }
    }
}
