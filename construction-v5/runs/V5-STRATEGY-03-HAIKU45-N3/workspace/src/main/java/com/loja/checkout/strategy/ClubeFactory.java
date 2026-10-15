package com.loja.checkout.strategy;

import com.loja.checkout.enums.NivelClube;

public class ClubeFactory {

    public static ClubeStrategy criar(NivelClube nivelClube) {
        return switch (nivelClube) {
            case BRONZE -> new ClubeBronze();
            case PRATA -> new ClubePrata();
            case OURO -> new ClubeOuro();
        };
    }
}
