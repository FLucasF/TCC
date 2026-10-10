package com.loja.checkout.estrategia;

import com.loja.checkout.domain.NivelClube;

public class FabricaClube {
    public static EstrategiaClube criar(NivelClube nivel) {
        return switch (nivel) {
            case BRONZE -> new ClubeNivelBronze();
            case PRATA -> new ClubeNivelPrata();
            case OURO -> new ClubeNivelOuro();
        };
    }
}
