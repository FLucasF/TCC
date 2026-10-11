package com.loja.checkout.clube;

import com.loja.checkout.model.NivelClube;

public class FabricaClube {
    public static BeneficiosClube criar(NivelClube nivel) {
        if (nivel == null) {
            return null;
        }

        return switch (nivel) {
            case BRONZE -> new ClubeBronze();
            case PRATA -> new ClubePrata();
            case OURO -> new ClubeOuro();
        };
    }
}
